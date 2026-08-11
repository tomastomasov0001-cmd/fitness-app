package com.harvis.fitnessapp.ui.exercises

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.databinding.ItemAllExerciseBinding

data class ExerciseWithCount(
    val exercise: Exercise,
    val variantCount: Int
)

class AllExercisesAdapter(
    private val onEditClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise, Int) -> Unit
) : ListAdapter<ExerciseWithCount, AllExercisesAdapter.ExerciseViewHolder>(ExerciseWithCountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemAllExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseViewHolder(
        private val binding: ItemAllExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exerciseWithCount: ExerciseWithCount) {
            val exercise = exerciseWithCount.exercise
            val count = exerciseWithCount.variantCount

            binding.exerciseName.text = exercise.name

            if (exercise.description.isNotEmpty()) {
                binding.exerciseDescription.text = exercise.description
                binding.exerciseDescription.visibility = View.VISIBLE
            } else {
                binding.exerciseDescription.visibility = View.GONE
            }

            // Show usage count
            val context = binding.root.context
            binding.usageCount.text = when (count) {
                0 -> context.getString(R.string.not_used)
                1 -> context.getString(R.string.used_in_one_workout)
                else -> context.getString(R.string.used_in_workouts, count)
            }

            // Show exercise type tags
            binding.tagSets.visibility = if (exercise.hasSets) View.VISIBLE else View.GONE
            binding.tagReps.visibility = if (exercise.hasReps) View.VISIBLE else View.GONE
            binding.tagWeight.visibility = if (exercise.hasWeight) View.VISIBLE else View.GONE
            binding.tagTime.visibility = if (exercise.hasTime) View.VISIBLE else View.GONE

            binding.editButton.setOnClickListener {
                onEditClick(exercise)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(exercise, count)
            }
        }
    }

    class ExerciseWithCountDiffCallback : DiffUtil.ItemCallback<ExerciseWithCount>() {
        override fun areItemsTheSame(oldItem: ExerciseWithCount, newItem: ExerciseWithCount): Boolean {
            return oldItem.exercise.id == newItem.exercise.id
        }

        override fun areContentsTheSame(oldItem: ExerciseWithCount, newItem: ExerciseWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
