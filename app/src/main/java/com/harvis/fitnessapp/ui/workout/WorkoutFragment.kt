package com.harvis.fitnessapp.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.databinding.FragmentWorkoutBinding

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var variantsAdapter: WorkoutVariantAdapter
    private var isViewOnly: Boolean = false
    private var isEditingCompleted: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[WorkoutViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVariantsList()
        setupButtons()
        observeViewModel()

        // Check if we should auto-start a workout from calendar
        arguments?.let { args ->
            val variantId = args.getLong("variantId", 0)
            val workoutLogId = args.getLong("workoutLogId", 0)
            val viewOnly = args.getBoolean("viewOnly", false)
            if (variantId > 0 && viewModel.isWorkoutActive.value != true) {
                if (viewOnly && workoutLogId > 0) {
                    viewModel.loadCompletedWorkout(variantId, workoutLogId)
                } else {
                    viewModel.startWorkoutById(variantId, workoutLogId)
                }
                // Clear arguments to prevent re-starting on config change
                arguments = null
            }
        }
    }

    private fun setupVariantsList() {
        variantsAdapter = WorkoutVariantAdapter { variant ->
            viewModel.startWorkout(variant)
        }
        binding.variantsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.variantsRecyclerView.adapter = variantsAdapter
    }

    private fun setupButtons() {
        binding.editWorkoutButton.setOnClickListener {
            viewModel.enableEditing()
        }

        binding.finishWorkoutButton.setOnClickListener {
            when {
                isViewOnly -> {
                    viewModel.closeViewMode()
                    findNavController().navigateUp()
                }
                isEditingCompleted -> {
                    viewModel.saveCompletedWorkout()
                    findNavController().navigateUp()
                }
                else -> {
                    showFinishDialog()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isWorkoutActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                binding.noWorkoutView.visibility = View.GONE
                binding.activeWorkoutView.visibility = View.VISIBLE
            } else {
                binding.noWorkoutView.visibility = View.VISIBLE
                binding.activeWorkoutView.visibility = View.GONE
            }
        }

        viewModel.allVariants.observe(viewLifecycleOwner) { variants ->
            variantsAdapter.submitList(variants)
            if (variants.isEmpty()) {
                binding.variantsRecyclerView.visibility = View.GONE
                binding.emptyVariantsView.visibility = View.VISIBLE
            } else {
                binding.variantsRecyclerView.visibility = View.VISIBLE
                binding.emptyVariantsView.visibility = View.GONE
            }
        }

        viewModel.currentVariant.observe(viewLifecycleOwner) { variant ->
            variant?.let {
                binding.workoutTitle.text = it.name
            }
        }

        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            refreshExercisesDisplay()
        }

        viewModel.isViewOnly.observe(viewLifecycleOwner) { viewOnly ->
            isViewOnly = viewOnly
            updateViewModeUI()
        }

        viewModel.isEditingCompleted.observe(viewLifecycleOwner) { editing ->
            isEditingCompleted = editing
            updateViewModeUI()
        }
    }

    private fun updateViewModeUI() {
        when {
            isViewOnly -> {
                binding.finishWorkoutButton.text = getString(R.string.back)
                binding.editWorkoutButton.visibility = View.VISIBLE
            }
            isEditingCompleted -> {
                binding.finishWorkoutButton.text = getString(R.string.save)
                binding.editWorkoutButton.visibility = View.GONE
            }
            else -> {
                binding.finishWorkoutButton.text = getString(R.string.finish)
                binding.editWorkoutButton.visibility = View.GONE
            }
        }
        // Refresh display to update edit/view state
        refreshExercisesDisplay()
    }

    private fun refreshExercisesDisplay() {
        val exercises = viewModel.exercises.value ?: return
        val container = binding.exercisesContainer
        container.removeAllViews()

        exercises.forEachIndexed { exerciseIndex, activeExercise ->
            val exerciseView = layoutInflater.inflate(R.layout.item_workout_exercise, container, false)

            val exerciseName = exerciseView.findViewById<TextView>(R.id.exerciseName)
            val exerciseDesc = exerciseView.findViewById<TextView>(R.id.exerciseDesc)
            val setsContainer = exerciseView.findViewById<LinearLayout>(R.id.setsContainer)
            val addSetButton = exerciseView.findViewById<MaterialButton>(R.id.addSetButton)

            exerciseName.text = "${exerciseIndex + 1}. ${activeExercise.exercise.name}"

            if (activeExercise.exercise.description.isNotEmpty()) {
                exerciseDesc.text = activeExercise.exercise.description
                exerciseDesc.visibility = View.VISIBLE
            } else {
                exerciseDesc.visibility = View.GONE
            }

            if (activeExercise.exercise.hasSets) {
                // Exercise with sets - show add button and set rows
                addSetButton.visibility = if (isViewOnly) View.GONE else View.VISIBLE
                addSetButton.setOnClickListener {
                    viewModel.addSet(exerciseIndex)
                }

                // Add sets
                activeExercise.sets.forEachIndexed { setIndex, setData ->
                    addSetView(setsContainer, exerciseIndex, setIndex, setData, activeExercise.exercise)
                }
            } else {
                // Exercise without sets - show simple completion view
                addSetButton.visibility = View.GONE
                addSimpleExerciseView(setsContainer, exerciseIndex, activeExercise)
            }

            container.addView(exerciseView)
        }
    }

    private fun addSetView(container: LinearLayout, exerciseIndex: Int, setIndex: Int, setData: SetData, exercise: com.harvis.fitnessapp.data.Exercise) {
        val setView = layoutInflater.inflate(R.layout.item_workout_set, container, false)

        val setNumber = setView.findViewById<TextView>(R.id.setNumber)
        val repsContainer = setView.findViewById<LinearLayout>(R.id.repsContainer)
        val weightContainer = setView.findViewById<LinearLayout>(R.id.weightContainer)
        val timeContainer = setView.findViewById<LinearLayout>(R.id.timeContainer)
        val repsInput = setView.findViewById<TextInputEditText>(R.id.repsInput)
        val weightInput = setView.findViewById<TextInputEditText>(R.id.weightInput)
        val hoursInput = setView.findViewById<TextInputEditText>(R.id.hoursInput)
        val minutesInput = setView.findViewById<TextInputEditText>(R.id.minutesInput)
        val secondsInput = setView.findViewById<TextInputEditText>(R.id.secondsInput)
        val completeButton = setView.findViewById<ImageButton>(R.id.completeButton)
        val cardView = setView as MaterialCardView

        setNumber.text = (setIndex + 1).toString()

        // Show/hide inputs based on exercise type
        repsContainer.visibility = if (exercise.hasReps) View.VISIBLE else View.GONE
        weightContainer.visibility = if (exercise.hasWeight) View.VISIBLE else View.GONE
        timeContainer.visibility = if (exercise.hasTime) View.VISIBLE else View.GONE

        // Set values
        repsInput.setText(if (setData.reps > 0) setData.reps.toString() else "")
        weightInput.setText(if (setData.weight > 0) setData.weight.toInt().toString() else "")

        // Set time values (convert seconds to hours:minutes:seconds)
        if (setData.timeSeconds > 0) {
            val hours = setData.timeSeconds / 3600
            val minutes = (setData.timeSeconds % 3600) / 60
            val seconds = setData.timeSeconds % 60
            hoursInput.setText(if (hours > 0) hours.toString() else "")
            minutesInput.setText(if (minutes > 0 || hours > 0) String.format("%02d", minutes) else "")
            secondsInput.setText(String.format("%02d", seconds))
        }

        // View only mode - disable editing
        if (isViewOnly) {
            repsInput.isEnabled = false
            weightInput.isEnabled = false
            hoursInput.isEnabled = false
            minutesInput.isEnabled = false
            secondsInput.isEnabled = false
            completeButton.visibility = View.GONE
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            repsInput.isEnabled = true
            weightInput.isEnabled = true
            hoursInput.isEnabled = true
            minutesInput.isEnabled = true
            secondsInput.isEnabled = true
            completeButton.visibility = View.VISIBLE

            // Visual completion state
            if (setData.completed) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            // Text watchers for reps and weight
            if (exercise.hasReps) {
                repsInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val reps = s.toString().toIntOrNull() ?: 0
                        val weight = weightInput.text.toString().toFloatOrNull() ?: 0f
                        viewModel.updateSet(exerciseIndex, setIndex, reps, weight)
                    }
                })
            }

            if (exercise.hasWeight) {
                weightInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val reps = repsInput.text.toString().toIntOrNull() ?: 0
                        val weight = s.toString().toFloatOrNull() ?: 0f
                        viewModel.updateSet(exerciseIndex, setIndex, reps, weight)
                    }
                })
            }

            // Text watchers for time
            if (exercise.hasTime) {
                val timeWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val hours = hoursInput.text.toString().toIntOrNull() ?: 0
                        val minutes = minutesInput.text.toString().toIntOrNull() ?: 0
                        val seconds = secondsInput.text.toString().toIntOrNull() ?: 0
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds
                        viewModel.updateSetTime(exerciseIndex, setIndex, totalSeconds)
                    }
                }
                hoursInput.addTextChangedListener(timeWatcher)
                minutesInput.addTextChangedListener(timeWatcher)
                secondsInput.addTextChangedListener(timeWatcher)
            }

            completeButton.setOnClickListener {
                viewModel.completeSet(exerciseIndex, setIndex)
                // Update visual state
                setData.completed = true
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }

        container.addView(setView)
    }

    private fun addSimpleExerciseView(container: LinearLayout, exerciseIndex: Int, activeExercise: ActiveExercise) {
        val exercise = activeExercise.exercise
        val setData = if (activeExercise.sets.isNotEmpty()) activeExercise.sets[0] else SetData()

        val setView = layoutInflater.inflate(R.layout.item_workout_set, container, false)

        val setNumber = setView.findViewById<TextView>(R.id.setNumber)
        val repsContainer = setView.findViewById<LinearLayout>(R.id.repsContainer)
        val weightContainer = setView.findViewById<LinearLayout>(R.id.weightContainer)
        val timeContainer = setView.findViewById<LinearLayout>(R.id.timeContainer)
        val repsInput = setView.findViewById<TextInputEditText>(R.id.repsInput)
        val weightInput = setView.findViewById<TextInputEditText>(R.id.weightInput)
        val hoursInput = setView.findViewById<TextInputEditText>(R.id.hoursInput)
        val minutesInput = setView.findViewById<TextInputEditText>(R.id.minutesInput)
        val secondsInput = setView.findViewById<TextInputEditText>(R.id.secondsInput)
        val completeButton = setView.findViewById<ImageButton>(R.id.completeButton)
        val cardView = setView as MaterialCardView

        // Hide set number for simple exercises (no sets)
        setNumber.visibility = View.GONE

        // Show inputs based on exercise type
        repsContainer.visibility = if (exercise.hasReps) View.VISIBLE else View.GONE
        weightContainer.visibility = if (exercise.hasWeight) View.VISIBLE else View.GONE
        timeContainer.visibility = if (exercise.hasTime) View.VISIBLE else View.GONE

        // Set values
        repsInput.setText(if (setData.reps > 0) setData.reps.toString() else "")
        weightInput.setText(if (setData.weight > 0) setData.weight.toInt().toString() else "")

        // Set time values
        if (setData.timeSeconds > 0) {
            val hours = setData.timeSeconds / 3600
            val minutes = (setData.timeSeconds % 3600) / 60
            val seconds = setData.timeSeconds % 60
            hoursInput.setText(if (hours > 0) hours.toString() else "")
            minutesInput.setText(if (minutes > 0 || hours > 0) String.format("%02d", minutes) else "")
            secondsInput.setText(String.format("%02d", seconds))
        }

        if (isViewOnly) {
            repsInput.isEnabled = false
            weightInput.isEnabled = false
            hoursInput.isEnabled = false
            minutesInput.isEnabled = false
            secondsInput.isEnabled = false
            completeButton.visibility = View.GONE
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            completeButton.visibility = View.VISIBLE

            if (setData.completed) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            // Reps watcher
            if (exercise.hasReps) {
                repsInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val reps = s.toString().toIntOrNull() ?: 0
                        val weight = weightInput.text.toString().toFloatOrNull() ?: 0f
                        viewModel.updateSet(exerciseIndex, 0, reps, weight)
                    }
                })
            }

            // Weight watcher
            if (exercise.hasWeight) {
                weightInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val reps = repsInput.text.toString().toIntOrNull() ?: 0
                        val weight = s.toString().toFloatOrNull() ?: 0f
                        viewModel.updateSet(exerciseIndex, 0, reps, weight)
                    }
                })
            }

            // Time watcher
            if (exercise.hasTime) {
                val timeWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val hours = hoursInput.text.toString().toIntOrNull() ?: 0
                        val minutes = minutesInput.text.toString().toIntOrNull() ?: 0
                        val secs = secondsInput.text.toString().toIntOrNull() ?: 0
                        val totalSeconds = hours * 3600 + minutes * 60 + secs
                        viewModel.updateSetTime(exerciseIndex, 0, totalSeconds)
                    }
                }
                hoursInput.addTextChangedListener(timeWatcher)
                minutesInput.addTextChangedListener(timeWatcher)
                secondsInput.addTextChangedListener(timeWatcher)
            }

            completeButton.setOnClickListener {
                viewModel.completeSet(exerciseIndex, 0)
                setData.completed = true
                cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                completeButton.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }

        container.addView(setView)
    }

    private fun showFinishDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.finish_workout_title)
            .setMessage(R.string.finish_workout_message)
            .setPositiveButton(R.string.finish) { _, _ ->
                viewModel.finishWorkout()
                findNavController().navigate(R.id.calendarFragment)
            }
            .setNegativeButton(R.string.cancel_workout) { _, _ ->
                viewModel.cancelWorkout()
                findNavController().navigate(R.id.calendarFragment)
            }
            .setNeutralButton(R.string.continue_text, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
