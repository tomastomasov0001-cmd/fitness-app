package com.harvis.fitnessapp.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.ExerciseSetWithDate
import com.harvis.fitnessapp.databinding.FragmentExerciseStatsBinding
import java.text.SimpleDateFormat
import java.util.*

class ExerciseStatsFragment : Fragment() {

    private var _binding: FragmentExerciseStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseStatsViewModel by viewModels()

    private var exerciseId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseId = arguments?.getLong("exerciseId") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadExerciseStats(exerciseId)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.exercise.observe(viewLifecycleOwner) { exercise ->
            exercise?.let {
                binding.tvExerciseName.text = it.name

                // Skryt/zobrazit kartu s vahou podle typu cviku
                binding.cardWeightProgress.visibility =
                    if (it.hasWeight) View.VISIBLE else View.GONE
                binding.layoutMaxWeight.visibility =
                    if (it.hasWeight) View.VISIBLE else View.GONE
                binding.layoutMaxReps.visibility =
                    if (it.hasReps) View.VISIBLE else View.GONE
            }
        }

        viewModel.allTimeBest.observe(viewLifecycleOwner) { (maxWeight, maxReps) ->
            binding.tvMaxWeight.text = "${maxWeight?.toInt() ?: 0} kg"
            binding.tvMaxReps.text = (maxReps ?: 0).toString()
        }

        viewModel.progressData.observe(viewLifecycleOwner) { data ->
            setupWeightChart(data)
            setupVolumeChart(data)
        }

        viewModel.lastWorkoutData.observe(viewLifecycleOwner) { sets ->
            setupLastWorkout(sets)
        }
    }

    private fun setupWeightChart(data: List<ProgressDataPoint>) {
        val chart = binding.chartWeightProgress

        if (data.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_data))
            chart.invalidate()
            return
        }

        val dateFormat = SimpleDateFormat("d.M.", Locale.getDefault())
        val labels = data.map { dateFormat.format(Date(it.date)) }

        val entries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.maxWeight)
        }

        val dataSet = LineDataSet(entries, getString(R.string.max_weight_kg)).apply {
            color = Color.parseColor("#2196F3")
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#2196F3"))
            setDrawValues(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.LINEAR
        }

        chart.apply {
            this.data = LineData(dataSet)
            description.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupVolumeChart(data: List<ProgressDataPoint>) {
        val chart = binding.chartVolumeProgress

        if (data.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_data))
            chart.invalidate()
            return
        }

        val dateFormat = SimpleDateFormat("d.M.", Locale.getDefault())
        val labels = data.map { dateFormat.format(Date(it.date)) }

        val entries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.totalVolume)
        }

        val dataSet = LineDataSet(entries, getString(R.string.volume)).apply {
            color = Color.parseColor("#4CAF50")
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#4CAF50"))
            setDrawValues(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 50
        }

        chart.apply {
            this.data = LineData(dataSet)
            description.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupLastWorkout(sets: List<ExerciseSetWithDate>) {
        val container = binding.layoutLastWorkoutSets
        container.removeAllViews()

        if (sets.isEmpty()) {
            binding.tvNoLastWorkout.visibility = View.VISIBLE
            binding.tvLastWorkoutDate.visibility = View.GONE
            return
        }

        binding.tvNoLastWorkout.visibility = View.GONE
        binding.tvLastWorkoutDate.visibility = View.VISIBLE

        val dateFormat = SimpleDateFormat("d. M. yyyy", Locale.getDefault())
        binding.tvLastWorkoutDate.text = dateFormat.format(Date(sets.first().workoutDate))

        val exercise = viewModel.exercise.value

        sets.forEachIndexed { index, set ->
            val setView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (4 * resources.displayMetrics.density).toInt()
                }
                textSize = 14f

                val parts = mutableListOf<String>()
                parts.add(getString(R.string.set_label, index + 1))

                if (exercise?.hasReps == true && set.reps > 0) {
                    parts.add("${set.reps} ${getString(R.string.reps_short)}")
                }
                if (exercise?.hasWeight == true && set.weight > 0) {
                    parts.add("${set.weight.toInt()} kg")
                }
                if (exercise?.hasTime == true && set.timeSeconds > 0) {
                    val hours = set.timeSeconds / 3600
                    val minutes = (set.timeSeconds % 3600) / 60
                    val seconds = set.timeSeconds % 60
                    if (hours > 0) {
                        parts.add(String.format("%d:%02d:%02d", hours, minutes, seconds))
                    } else {
                        parts.add(String.format("%d:%02d", minutes, seconds))
                    }
                }

                text = parts.joinToString(" ")
            }
            container.addView(setView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
