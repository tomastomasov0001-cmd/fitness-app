package com.harvis.fitnessapp.ui.calendar

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.WorkoutLog
import com.harvis.fitnessapp.data.WorkoutVariant
import com.harvis.fitnessapp.databinding.FragmentCalendarBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthScrollListener
import com.kizitonwose.calendar.view.ViewContainer
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CalendarViewModel
    private var selectedDate: LocalDate = LocalDate.now()
    private var cachedVariants: List<WorkoutVariant> = emptyList()
    private var currentWorkoutLogs: List<WorkoutLog> = emptyList()
    private var workoutLogsForMonth: Map<LocalDate, List<WorkoutLog>> = emptyMap()

    private val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale("cs", "CZ"))
    private val monthYearFormat = SimpleDateFormat("LLLL yyyy", Locale("cs", "CZ"))

    companion object {
        val VARIANT_COLORS = listOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFFF9800.toInt(), // Orange
            0xFF9C27B0.toInt(), // Purple
            0xFFE91E63.toInt(), // Pink
            0xFF00BCD4.toInt(), // Cyan
            0xFFFF5722.toInt(), // Deep Orange
            0xFF3F51B5.toInt()  // Indigo
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupButtons()
        observeData()
        updateSelectedDateDisplay()
        loadWorkoutsForSelectedDate()
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = DayOfWeek.MONDAY

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.textView
                val indicators = container.indicators

                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE

                    // Highlight selected day
                    if (data.date == selectedDate) {
                        textView.setBackgroundResource(R.drawable.circle_primary)
                        textView.setTextColor(resources.getColor(android.R.color.white, null))
                    } else if (data.date == LocalDate.now()) {
                        textView.setBackgroundResource(R.drawable.circle_today)
                        textView.setTextColor(resources.getColor(R.color.primary, null))
                    } else {
                        textView.background = null
                        textView.setTextColor(resources.getColor(R.color.on_surface, null))
                    }

                    // Show workout indicators (up to 4)
                    val workoutLogs = workoutLogsForMonth[data.date] ?: emptyList()
                    indicators.forEachIndexed { index, indicator ->
                        if (index < workoutLogs.size) {
                            indicator.visibility = View.VISIBLE
                            val log = workoutLogs[index]
                            val variantIndex = cachedVariants.indexOfFirst { it.id == log.variantId }
                            val color = if (variantIndex >= 0) {
                                VARIANT_COLORS[variantIndex % VARIANT_COLORS.size]
                            } else {
                                VARIANT_COLORS[0]
                            }
                            val drawable = GradientDrawable()
                            drawable.shape = GradientDrawable.OVAL
                            drawable.setColor(color)
                            indicator.background = drawable
                        } else {
                            indicator.visibility = View.GONE
                        }
                    }
                } else {
                    textView.visibility = View.INVISIBLE
                    indicators.forEach { it.visibility = View.GONE }
                }
            }
        }

        binding.calendarView.monthScrollListener = object : MonthScrollListener {
            override fun invoke(month: CalendarMonth) {
                updateMonthYearText(month.yearMonth)
                loadWorkoutsForMonth(month.yearMonth)
            }
        }

        // Initial month text
        updateMonthYearText(currentMonth)
        loadWorkoutsForMonth(currentMonth)

        // Month navigation
        binding.prevMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        binding.nextMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
    }

    private fun updateMonthYearText(yearMonth: YearMonth) {
        val date = Date.from(yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
        binding.monthYearText.text = monthYearFormat.format(date).replaceFirstChar { it.uppercaseChar() }
    }

    private fun loadWorkoutsForMonth(yearMonth: YearMonth) {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonth = yearMonth.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 24 * 60 * 60 * 1000 - 1

        viewModel.getWorkoutLogsForMonth(startOfMonth, endOfMonth).observe(viewLifecycleOwner) { logs ->
            workoutLogsForMonth = logs.groupBy { log ->
                java.time.Instant.ofEpochMilli(log.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }.mapValues { it.value.take(4) }
            binding.calendarView.notifyCalendarChanged()
        }
    }

    private fun setupButtons() {
        binding.assignWorkoutButton.setOnClickListener {
            showAssignWorkoutDialog()
        }
    }

    private fun observeData() {
        viewModel.allVariants.observe(viewLifecycleOwner) { variants ->
            cachedVariants = variants
            binding.calendarView.notifyCalendarChanged()
            updateWorkoutsDisplay()
        }

        viewModel.workoutsForDate.observe(viewLifecycleOwner) { workoutLogs ->
            currentWorkoutLogs = workoutLogs
            updateWorkoutsDisplay()
        }
    }

    private fun updateWorkoutsDisplay() {
        binding.workoutsContainer.removeAllViews()

        if (currentWorkoutLogs.isEmpty()) {
            binding.noWorkoutText.visibility = View.VISIBLE
            binding.assignWorkoutButton.text = getString(R.string.assign_workout)
        } else {
            binding.noWorkoutText.visibility = View.GONE

            // Check if we can add more workouts (max 4)
            if (currentWorkoutLogs.size >= 4) {
                binding.assignWorkoutButton.visibility = View.GONE
            } else {
                binding.assignWorkoutButton.visibility = View.VISIBLE
                binding.assignWorkoutButton.text = "Přidat další trénink"
            }

            currentWorkoutLogs.forEach { log ->
                val itemView = layoutInflater.inflate(R.layout.item_calendar_workout, binding.workoutsContainer, false)
                val colorIndicator = itemView.findViewById<View>(R.id.colorIndicator)
                val workoutName = itemView.findViewById<TextView>(R.id.workoutName)
                val startButton = itemView.findViewById<MaterialButton>(R.id.startButton)
                val viewButton = itemView.findViewById<MaterialButton>(R.id.viewButton)
                val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)

                val variantIndex = cachedVariants.indexOfFirst { it.id == log.variantId }
                val variant = cachedVariants.find { it.id == log.variantId }

                if (variant != null && variantIndex >= 0) {
                    val colorIndex = variantIndex % VARIANT_COLORS.size
                    val color = VARIANT_COLORS[colorIndex]

                    val drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.OVAL
                    drawable.setColor(color)
                    colorIndicator.background = drawable

                    val statusText = if (log.completed) "✓ " else ""
                    workoutName.text = "$statusText${variantIndex + 1}. ${variant.name}"

                    if (log.completed) {
                        // Dokončený trénink - zobrazit tlačítko Editovat
                        startButton.visibility = View.GONE
                        viewButton.visibility = View.VISIBLE
                        viewButton.text = "Editovat"
                        viewButton.setOnClickListener {
                            val bundle = bundleOf(
                                "variantId" to log.variantId,
                                "workoutLogId" to log.id,
                                "viewOnly" to true  // This triggers loadCompletedWorkout which enables editing
                            )
                            findNavController().navigate(R.id.workoutFragment, bundle)
                        }
                    } else {
                        // Nedokončený trénink - zobrazit tlačítko Start
                        startButton.visibility = View.VISIBLE
                        viewButton.visibility = View.GONE
                        startButton.setOnClickListener {
                            val bundle = bundleOf(
                                "variantId" to log.variantId,
                                "workoutLogId" to log.id
                            )
                            findNavController().navigate(R.id.workoutFragment, bundle)
                        }
                    }

                    // Tlačítko pro smazání
                    deleteButton.setOnClickListener {
                        showDeleteConfirmDialog(log, variant.name)
                    }
                } else {
                    workoutName.text = "Neznámý trénink"
                    colorIndicator.visibility = View.GONE
                    startButton.visibility = View.GONE
                    viewButton.visibility = View.GONE
                    deleteButton.setOnClickListener {
                        showDeleteConfirmDialog(log, "Neznámý trénink")
                    }
                }

                binding.workoutsContainer.addView(itemView)
            }
        }
    }

    private fun updateSelectedDateDisplay() {
        val today = LocalDate.now()
        val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (selectedDate == today) {
            binding.selectedDateText.text = "Dnes - ${dateFormat.format(Date(dateMillis))}"
        } else {
            binding.selectedDateText.text = dateFormat.format(Date(dateMillis))
        }
    }

    private fun loadWorkoutsForSelectedDate() {
        val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModel.loadWorkoutsForDate(dateMillis)
    }

    private fun showDeleteConfirmDialog(log: WorkoutLog, workoutName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Odebrat trénink")
            .setMessage("Opravdu chcete odebrat trénink \"$workoutName\" z tohoto dne?")
            .setPositiveButton("Odebrat") { _, _ ->
                val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                viewModel.deleteWorkoutLog(log.id, dateMillis)
                // Refresh calendar to update indicators
                binding.calendarView.findFirstVisibleMonth()?.let { month ->
                    loadWorkoutsForMonth(month.yearMonth)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAssignWorkoutDialog() {
        if (cachedVariants.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Žádné tréninky")
                .setMessage("Nejprve vytvořte trénink v záložce 'Tréninky'.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (currentWorkoutLogs.size >= 4) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Limit dosažen")
                .setMessage("Na jeden den lze přidat maximálně 4 tréninky.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val variantNames = cachedVariants.mapIndexed { index, variant ->
            "${index + 1}. ${variant.name}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.assign_workout)
            .setItems(variantNames) { _, which ->
                val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                viewModel.assignWorkoutToDate(cachedVariants[which].id, dateMillis)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendarDayText)
        val indicators: List<View> = listOf(
            view.findViewById(R.id.indicator1),
            view.findViewById(R.id.indicator2),
            view.findViewById(R.id.indicator3),
            view.findViewById(R.id.indicator4)
        )
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    val oldDate = selectedDate
                    selectedDate = day.date
                    binding.calendarView.notifyDateChanged(oldDate)
                    binding.calendarView.notifyDateChanged(day.date)
                    updateSelectedDateDisplay()
                    loadWorkoutsForSelectedDate()
                }
            }
        }
    }
}
