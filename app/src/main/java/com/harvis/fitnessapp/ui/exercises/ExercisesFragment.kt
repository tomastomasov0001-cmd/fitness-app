package com.harvis.fitnessapp.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.data.ExercisesBackupHelper
import com.harvis.fitnessapp.databinding.FragmentExercisesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExercisesFragment : Fragment() {

    private var _binding: FragmentExercisesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExercisesViewModel
    private lateinit var adapter: AllExercisesAdapter

    private var currentExercises: List<Exercise> = emptyList()

    // Export launcher
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportToFile(it) }
    }

    // Import launcher
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ExercisesViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()
        setupFab()
        setupExportImport()
    }

    private fun setupExportImport() {
        binding.btnExport.setOnClickListener {
            if (currentExercises.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_exercises_to_export, Toast.LENGTH_SHORT).show()
            } else {
                exportLauncher.launch("exercises_backup.json")
            }
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch("application/json")
        }
    }

    private fun exportToFile(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val backup = ExercisesBackupHelper.createBackup(currentExercises)
                val json = ExercisesBackupHelper.toJson(backup)

                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.exercises_export_success, currentExercises.size),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_saving, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFromFile(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                }

                if (json == null) {
                    Toast.makeText(requireContext(), R.string.error_reading_file, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val backup = ExercisesBackupHelper.fromJson(json)
                if (backup == null) {
                    Toast.makeText(requireContext(), R.string.invalid_file_format, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val exercises = ExercisesBackupHelper.toExercises(backup)
                val existingNames = currentExercises.map { it.name.lowercase() }.toSet()
                var importedCount = 0
                var skippedCount = 0

                for (exercise in exercises) {
                    if (exercise.name.lowercase() !in existingNames) {
                        viewModel.insertExercise(exercise)
                        importedCount++
                    } else {
                        skippedCount++
                    }
                }

                val message = if (skippedCount > 0) {
                    getString(R.string.exercises_import_with_skipped, importedCount, skippedCount)
                } else {
                    getString(R.string.exercises_import_success, importedCount)
                }

                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_reading_file, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AllExercisesAdapter(
            onEditClick = { exercise ->
                showEditExerciseDialog(exercise)
            },
            onDeleteClick = { exercise, variantCount ->
                showDeleteDialog(exercise, variantCount)
            }
        )
        binding.exercisesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.exercisesRecyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.allExercises.observe(viewLifecycleOwner) { exercises ->
            currentExercises = exercises
            binding.emptyView.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE

            // Load variant counts for each exercise
            lifecycleScope.launch {
                val exercisesWithCount = exercises.map { exercise ->
                    val count = viewModel.getVariantCountForExercise(exercise.id)
                    ExerciseWithCount(exercise, count)
                }
                adapter.submitList(exercisesWithCount)
            }
        }
    }

    private fun setupFab() {
        binding.fabAddExercise.setOnClickListener {
            showAddExerciseDialog()
        }
    }

    private fun showAddExerciseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.exerciseNameInput)
        val nameInputLayout = nameInput.parent.parent as TextInputLayout
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.exerciseDescInput)
        val setsInputLayout = dialogView.findViewById<TextInputLayout>(R.id.setsInputLayout)
        val defaultSetsInput = dialogView.findViewById<TextInputEditText>(R.id.defaultSetsInput)
        val hasSetsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasSetsCheckbox)
        val hasRepsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasRepsCheckbox)
        val hasWeightCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasWeightCheckbox)
        val hasTimeCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasTimeCheckbox)

        // Show/hide sets input based on checkbox
        hasSetsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            setsInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_exercise)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    nameInputLayout.error = getString(R.string.enter_exercise_name)
                    Toast.makeText(requireContext(), getString(R.string.enter_exercise_name), Toast.LENGTH_SHORT).show()
                } else {
                    nameInputLayout.error = null
                    val defaultSets = defaultSetsInput.text.toString().toIntOrNull() ?: 3
                    val exercise = Exercise(
                        name = name,
                        description = descInput.text.toString().trim(),
                        hasSets = hasSetsCheckbox.isChecked,
                        hasReps = hasRepsCheckbox.isChecked,
                        hasWeight = hasWeightCheckbox.isChecked,
                        hasTime = hasTimeCheckbox.isChecked,
                        defaultSets = defaultSets
                    )
                    viewModel.insertExercise(exercise)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showEditExerciseDialog(exercise: Exercise) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.exerciseNameInput)
        val nameInputLayout = nameInput.parent.parent as TextInputLayout
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.exerciseDescInput)
        val setsInputLayout = dialogView.findViewById<TextInputLayout>(R.id.setsInputLayout)
        val defaultSetsInput = dialogView.findViewById<TextInputEditText>(R.id.defaultSetsInput)
        val hasSetsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasSetsCheckbox)
        val hasRepsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasRepsCheckbox)
        val hasWeightCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasWeightCheckbox)
        val hasTimeCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.hasTimeCheckbox)

        // Pre-fill current values
        nameInput.setText(exercise.name)
        descInput.setText(exercise.description)
        hasSetsCheckbox.isChecked = exercise.hasSets
        hasRepsCheckbox.isChecked = exercise.hasReps
        hasWeightCheckbox.isChecked = exercise.hasWeight
        hasTimeCheckbox.isChecked = exercise.hasTime
        defaultSetsInput.setText(exercise.defaultSets.toString())

        // Set initial visibility
        setsInputLayout.visibility = if (exercise.hasSets) View.VISIBLE else View.GONE

        // Show/hide sets input based on checkbox
        hasSetsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            setsInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    nameInputLayout.error = getString(R.string.enter_exercise_name)
                    Toast.makeText(requireContext(), getString(R.string.enter_exercise_name), Toast.LENGTH_SHORT).show()
                } else {
                    nameInputLayout.error = null
                    val defaultSets = defaultSetsInput.text.toString().toIntOrNull() ?: 3
                    val updatedExercise = exercise.copy(
                        name = name,
                        description = descInput.text.toString().trim(),
                        hasSets = hasSetsCheckbox.isChecked,
                        hasReps = hasRepsCheckbox.isChecked,
                        hasWeight = hasWeightCheckbox.isChecked,
                        hasTime = hasTimeCheckbox.isChecked,
                        defaultSets = defaultSets
                    )
                    viewModel.updateExercise(updatedExercise)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(exercise: Exercise, variantCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_warning_title)
            .setMessage(getString(R.string.delete_exercise_warning, exercise.name))
            .setPositiveButton(R.string.delete_anyway) { _, _ ->
                viewModel.deleteExercise(exercise)
            }
            .setNeutralButton(R.string.export_first) { _, _ ->
                // Spustit export cviků
                if (currentExercises.isNotEmpty()) {
                    exportLauncher.launch("exercises_backup.json")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
