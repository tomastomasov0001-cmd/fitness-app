package com.harvis.fitnessapp.ui.variants

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.data.VariantWithCount
import com.harvis.fitnessapp.databinding.ItemVariantBinding

class VariantsAdapter(
    private val onItemClick: (VariantWithCount) -> Unit,
    private val onEditClick: (VariantWithCount) -> Unit,
    private val onDeleteClick: (VariantWithCount) -> Unit
) : ListAdapter<VariantWithCount, VariantsAdapter.ViewHolder>(DiffCallback()) {

    companion object {
        val VARIANT_COLORS = listOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFFF9800.toInt(), // Orange
            0xFF9C27B0.toInt(), // Purple
            0xFFE91E63.toInt(), // Pink
            0xFF00BCD4.toInt(), // Cyan
            0xFFFF5722.toInt(), // Deep Orange
            0xFF3F51B5.toInt()  // Indigo
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVariantBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(
        private val binding: ItemVariantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(variant: VariantWithCount, number: Int) {
            binding.variantNumber.text = number.toString()
            binding.variantName.text = variant.name

            // Set color for number circle
            val colorIndex = (number - 1) % VARIANT_COLORS.size
            val color = VARIANT_COLORS[colorIndex]
            val drawable = binding.variantNumber.background as? GradientDrawable
                ?: GradientDrawable().also { binding.variantNumber.background = it }
            drawable.setColor(color)
            drawable.shape = GradientDrawable.OVAL
            binding.variantDescription.text = variant.description.ifEmpty { "Žádný popis" }

            val countText = when {
                variant.exerciseCount == 0 -> "Žádné cviky"
                variant.exerciseCount == 1 -> "1 cvik"
                variant.exerciseCount in 2..4 -> "${variant.exerciseCount} cviky"
                else -> "${variant.exerciseCount} cviků"
            }
            binding.exerciseCount.text = countText

            binding.root.setOnClickListener {
                onItemClick(variant)
            }

            binding.editButton.setOnClickListener {
                onEditClick(variant)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(variant)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VariantWithCount>() {
        override fun areItemsTheSame(oldItem: VariantWithCount, newItem: VariantWithCount): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VariantWithCount, newItem: VariantWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
