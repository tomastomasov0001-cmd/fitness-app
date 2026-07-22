package com.harvis.fitnessapp.ui.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.R

class RecordsAdapter : ListAdapter<ExerciseRecord, RecordsAdapter.RecordViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        private val tvMaxWeight: TextView = itemView.findViewById(R.id.tvMaxWeight)
        private val tvMaxReps: TextView = itemView.findViewById(R.id.tvMaxReps)
        private val layoutWeight: LinearLayout = itemView.findViewById(R.id.layoutWeight)
        private val layoutReps: LinearLayout = itemView.findViewById(R.id.layoutReps)

        fun bind(record: ExerciseRecord) {
            tvExerciseName.text = record.exercise.name

            // Zobrazit vahu pouze pokud cvik ma vahu
            if (record.exercise.hasWeight && record.maxWeight != null && record.maxWeight > 0) {
                layoutWeight.visibility = View.VISIBLE
                tvMaxWeight.text = "${record.maxWeight.toInt()} kg"
            } else {
                layoutWeight.visibility = View.GONE
            }

            // Zobrazit opakovani pouze pokud cvik ma opakovani
            if (record.exercise.hasReps && record.maxReps != null && record.maxReps > 0) {
                layoutReps.visibility = View.VISIBLE
                tvMaxReps.text = record.maxReps.toString()
            } else {
                layoutReps.visibility = View.GONE
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<ExerciseRecord>() {
        override fun areItemsTheSame(oldItem: ExerciseRecord, newItem: ExerciseRecord): Boolean {
            return oldItem.exercise.id == newItem.exercise.id
        }

        override fun areContentsTheSame(oldItem: ExerciseRecord, newItem: ExerciseRecord): Boolean {
            return oldItem == newItem
        }
    }
}
