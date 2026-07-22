package com.harvis.fitnessapp.ui.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.Exercise

class ExerciseSelectAdapter(
    private val onItemClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseSelectAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_select, parent, false)
        return ExerciseViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExerciseViewHolder(
        itemView: View,
        private val onItemClick: (Exercise) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)

        fun bind(exercise: Exercise) {
            tvExerciseName.text = exercise.name
            itemView.setOnClickListener { onItemClick(exercise) }
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
