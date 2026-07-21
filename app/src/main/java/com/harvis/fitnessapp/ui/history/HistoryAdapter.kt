package com.harvis.fitnessapp.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.data.WorkoutLogWithDetails
import com.harvis.fitnessapp.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (WorkoutLogWithDetails) -> Unit
) : ListAdapter<WorkoutLogWithDetails, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale("cs", "CZ"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WorkoutLogWithDetails) {
            binding.workoutName.text = item.variant.name
            binding.workoutDate.text = dateFormat.format(Date(item.log.date))

            if (item.log.completed) {
                binding.completedBadge.visibility = View.VISIBLE
            } else {
                binding.completedBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<WorkoutLogWithDetails>() {
        override fun areItemsTheSame(oldItem: WorkoutLogWithDetails, newItem: WorkoutLogWithDetails): Boolean {
            return oldItem.log.id == newItem.log.id
        }

        override fun areContentsTheSame(oldItem: WorkoutLogWithDetails, newItem: WorkoutLogWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}
