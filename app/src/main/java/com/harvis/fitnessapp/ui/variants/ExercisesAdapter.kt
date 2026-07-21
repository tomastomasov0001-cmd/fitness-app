package com.harvis.fitnessapp.ui.variants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.databinding.ItemExerciseBinding

class ExercisesAdapter(
    private val onEditClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExercisesAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.exerciseName.text = exercise.name

            if (exercise.description.isNotEmpty()) {
                binding.exerciseDescription.text = exercise.description
                binding.exerciseDescription.visibility = View.VISIBLE
            } else {
                binding.exerciseDescription.visibility = View.GONE
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
                onDeleteClick(exercise)
            }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}
