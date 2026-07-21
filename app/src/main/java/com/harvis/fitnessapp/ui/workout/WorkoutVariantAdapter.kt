package com.harvis.fitnessapp.ui.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.WorkoutVariant

class WorkoutVariantAdapter(
    private val onItemClick: (WorkoutVariant) -> Unit
) : ListAdapter<WorkoutVariant, WorkoutVariantAdapter.VariantViewHolder>(VariantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_variant, parent, false)
        return VariantViewHolder(view)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VariantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val variantName: TextView = itemView.findViewById(R.id.variantName)
        private val variantDesc: TextView = itemView.findViewById(R.id.variantDesc)

        fun bind(variant: WorkoutVariant) {
            variantName.text = variant.name

            if (variant.description.isNotEmpty()) {
                variantDesc.text = variant.description
                variantDesc.visibility = View.VISIBLE
            } else {
                variantDesc.visibility = View.GONE
            }

            cardView.setOnClickListener {
                onItemClick(variant)
            }
        }
    }

    class VariantDiffCallback : DiffUtil.ItemCallback<WorkoutVariant>() {
        override fun areItemsTheSame(oldItem: WorkoutVariant, newItem: WorkoutVariant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutVariant, newItem: WorkoutVariant): Boolean {
            return oldItem == newItem
        }
    }
}
