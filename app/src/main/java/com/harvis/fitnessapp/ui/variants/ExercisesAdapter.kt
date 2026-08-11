package com.harvis.fitnessapp.ui.variants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.databinding.ItemExerciseBinding

class ExercisesAdapter(
    private val onEditClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit,
    private val onOrderChanged: ((List<Exercise>) -> Unit)? = null
) : RecyclerView.Adapter<ExercisesAdapter.ExerciseViewHolder>() {

    private val items = mutableListOf<Exercise>()
    private var isDragging = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<Exercise>?) {
        if (isDragging) return // Ignore updates while dragging

        // Check if list is the same (same items in same order)
        val newList = list ?: emptyList()
        if (items.size == newList.size && items.map { it.id } == newList.map { it.id }) {
            // Same order, just update content if needed
            var hasChanges = false
            for (i in items.indices) {
                if (items[i] != newList[i]) {
                    items[i] = newList[i]
                    hasChanges = true
                }
            }
            if (hasChanges) {
                notifyDataSetChanged()
            }
            return
        }

        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        isDragging = true
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                java.util.Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                java.util.Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun onMoveFinished() {
        onOrderChanged?.invoke(items.toList())
        isDragging = false
    }

    fun getItems(): List<Exercise> = items.toList()

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
}
