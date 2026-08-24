package com.harvis.fitnessapp.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.DateWorkoutCount
import com.harvis.fitnessapp.data.VariantWorkoutCount
import com.harvis.fitnessapp.databinding.FragmentStatisticsBinding
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var recordsAdapter: RecordsAdapter
    private lateinit var exerciseAdapter: ExerciseSelectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAllStatistics()
    }

    private fun setupRecyclerViews() {
        recordsAdapter = RecordsAdapter()
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordsAdapter
        }

        exerciseAdapter = ExerciseSelectAdapter { exercise ->
            findNavController().navigate(
                R.id.action_statistics_to_exerciseStats,
                bundleOf("exerciseId" to exercise.id)
            )
        }
        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvTotalWorkouts.text = it.totalWorkouts.toString()
                binding.tvTrainingDays.text = it.trainingDaysThisMonth.toString()
                binding.tvCurrentStreak.text = it.currentStreak.toString()
                binding.tvBestStreak.text = it.bestStreak.toString()
            }
        }

        viewModel.weeklyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvWeekWorkouts.text = it.workoutsThisWeek.toString()
                binding.tvWeekVolume.text = it.totalVolume.toInt().toString()
                binding.tvWeekSets.text = it.totalSets.toString()
            }
        }

        viewModel.monthlyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvMonthWorkouts.text = it.workoutsThisMonth.toString()
                val changeText = when {
                    it.change > 0 -> getString(R.string.compared_to_last_month_positive, it.change)
                    it.change < 0 -> getString(R.string.compared_to_last_month_negative, it.change)
                    else -> getString(R.string.same_as_last_month)
                }
                binding.tvMonthChange.text = changeText
                binding.tvMonthChange.setTextColor(
                    when {
                        it.change > 0 -> Color.parseColor("#4CAF50")
                        it.change < 0 -> Color.parseColor("#F44336")
                        else -> Color.GRAY
                    }
                )
            }
        }

        viewModel.variantDistribution.observe(viewLifecycleOwner) { distribution ->
            distribution?.let { setupPieChart(it) }
        }

        viewModel.workoutFrequency.observe(viewLifecycleOwner) { frequency ->
            frequency?.let { setupBarChart(it) }
        }

        viewModel.heatmapData.observe(viewLifecycleOwner) { data ->
            data?.let { setupHeatmap(it) }
        }

        viewModel.exerciseRecords.observe(viewLifecycleOwner) { records ->
            val list = records ?: emptyList()
            if (list.isEmpty()) {
                binding.rvRecords.visibility = View.GONE
                binding.tvNoRecords.visibility = View.VISIBLE
            } else {
                binding.rvRecords.visibility = View.VISIBLE
                binding.tvNoRecords.visibility = View.GONE
                recordsAdapter.submitList(list)
            }
        }

        viewModel.exercisesWithData.observe(viewLifecycleOwner) { exercises ->
            val list = exercises ?: emptyList()
            if (list.isEmpty()) {
                binding.rvExercises.visibility = View.GONE
                binding.tvNoExercises.visibility = View.VISIBLE
            } else {
                binding.rvExercises.visibility = View.VISIBLE
                binding.tvNoExercises.visibility = View.GONE
                exerciseAdapter.submitList(list)
            }
        }
    }

    private fun setupPieChart(distribution: List<VariantWorkoutCount>) {
        val chart = binding.chartDistribution

        if (distribution.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_data))
            chart.invalidate()
            return
        }

        val entries = distribution.map { PieEntry(it.count.toFloat(), it.variantName) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(frequency: List<DateWorkoutCount>) {
        val chart = binding.chartFrequency

        if (frequency.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_data))
            chart.invalidate()
            return
        }

        // Seskupit podle tydnu
        val weeklyData = mutableMapOf<String, Int>()
        val dateFormat = SimpleDateFormat("d.M.", Locale.getDefault())

        frequency.forEach { item ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = item.date
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val weekStart = dateFormat.format(calendar.time)
            weeklyData[weekStart] = (weeklyData[weekStart] ?: 0) + item.count
        }

        val entries = weeklyData.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, getString(R.string.workouts_count)).apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
            valueTextSize = 10f
        }

        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(weeklyData.keys.toList())
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupHeatmap(data: List<DateWorkoutCount>) {
        val container = binding.heatmapContainer
        container.removeAllViews()

        val context = requireContext()

        // Vytvorit mapu datum -> pocet treninku
        val workoutMap = data.associate { it.date to it.count }

        // Ziskat posledni rok
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -1)
        val startDate = calendar.timeInMillis

        // Velikost ctverecku
        val cellSize = (16 * resources.displayMetrics.density).toInt()
        val cellMargin = (2 * resources.displayMetrics.density).toInt()

        // Vytvorit sloupce pro kazdy tyden
        calendar.timeInMillis = startDate
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        while (calendar.timeInMillis <= endDate) {
            val weekColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // 7 dni v tydnu
            for (day in 0..6) {
                val dayCalendar = Calendar.getInstance()
                dayCalendar.timeInMillis = calendar.timeInMillis
                dayCalendar.add(Calendar.DAY_OF_YEAR, day)

                // Normalizovat datum
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                dayCalendar.set(Calendar.MINUTE, 0)
                dayCalendar.set(Calendar.SECOND, 0)
                dayCalendar.set(Calendar.MILLISECOND, 0)

                val dayTimestamp = dayCalendar.timeInMillis
                val count = workoutMap[dayTimestamp] ?: 0

                val cellView = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(cellSize, cellSize).apply {
                        setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
                    }
                    setBackgroundColor(getHeatmapColor(count))
                }

                weekColumn.addView(cellView)
            }

            container.addView(weekColumn)
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }

    private fun getHeatmapColor(count: Int): Int {
        return when {
            count == 0 -> Color.parseColor("#EBEDF0")
            count == 1 -> Color.parseColor("#9BE9A8")
            count == 2 -> Color.parseColor("#40C463")
            count == 3 -> Color.parseColor("#30A14E")
            else -> Color.parseColor("#216E39")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
