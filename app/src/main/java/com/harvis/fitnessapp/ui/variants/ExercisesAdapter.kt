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
    private var lastOrderIds: List<Long> = emptyList()

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

        val newList = list ?: emptyList()
        val newIds = newList.map { it.id }

        // If this matches our last drag order, just accept silently
        if (newIds == lastOrderIds) {
            lastOrderIds = emptyList()
            // Update items without notifying (data is same, just from DB now)
            items.clear()
            items.addAll(newList)
            return
        }

        // Check if list is the same as current
        val currentIds = items.map { it.id }
        if (currentIds == newIds) {
            // Same order, no need to refresh
            items.clear()
            items.addAll(newList)
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
        lastOrderIds = items.map { it.id }
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
