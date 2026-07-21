package com.harvis.fitnessapp.ui.workout

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.databinding.ItemWorkoutSetBinding

class SetsAdapter(
    private val onSetChanged: (Int, Int, Float) -> Unit,
    private val onSetCompleted: (Int) -> Unit
) : RecyclerView.Adapter<SetsAdapter.SetViewHolder>() {

    private var sets: List<SetData> = emptyList()
    private var currentListId: Int = 0
    private var isViewOnly: Boolean = false

    fun setViewOnly(viewOnly: Boolean) {
        if (isViewOnly != viewOnly) {
            isViewOnly = viewOnly
            notifyDataSetChanged()
        }
    }

    fun submitList(newSets: List<SetData>, forceRefresh: Boolean = false) {
        val oldSize = sets.size
        val newListId = System.identityHashCode(newSets)

        // Aktualizovat pouze pokud se zmenil seznam (jiny cvik) nebo velikost
        val shouldRefresh = forceRefresh ||
                            oldSize != newSets.size ||
                            currentListId != newListId ||
                            oldSize == 0

        sets = newSets
        currentListId = newListId

        if (shouldRefresh) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val binding = ItemWorkoutSetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(sets[position], position, isViewOnly)
    }

    override fun getItemCount(): Int = sets.size

    inner class SetViewHolder(
        private val binding: ItemWorkoutSetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var repsWatcher: TextWatcher? = null
        private var weightWatcher: TextWatcher? = null
        private var currentPosition: Int = 0

        fun bind(setData: SetData, position: Int, viewOnly: Boolean) {
            currentPosition = position

            // Odstranit stare listenery
            repsWatcher?.let { binding.repsInput.removeTextChangedListener(it) }
            weightWatcher?.let { binding.weightInput.removeTextChangedListener(it) }

            binding.setNumber.text = (position + 1).toString()

            // Nastavit hodnoty bez spusteni listeneru
            binding.repsInput.setText(if (setData.reps > 0) setData.reps.toString() else "")
            binding.weightInput.setText(if (setData.weight > 0) setData.weight.toInt().toString() else "")

            // Rezim pouze zobrazeni
            if (viewOnly) {
                binding.repsInput.isEnabled = false
                binding.weightInput.isEnabled = false
                binding.completeButton.visibility = android.view.View.GONE

                // Vsechny serie zobrazit jako dokoncene v rezimu prohlizeni
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.success)
                )
                return
            }

            // Normalni rezim - povolit editaci
            binding.repsInput.isEnabled = true
            binding.weightInput.isEnabled = true
            binding.completeButton.visibility = android.view.View.VISIBLE

            // Vizualni stav dokonceni
            if (setData.completed) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.success)
                )
                binding.completeButton.setColorFilter(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface)
                )
                binding.completeButton.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                )
            }

            // Nove listenery
            repsWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val reps = s.toString().toIntOrNull() ?: 0
                        val weight = binding.weightInput.text.toString().toFloatOrNull() ?: 0f
                        onSetChanged(currentPosition, reps, weight)
                    } catch (e: Exception) {
                        // Ignorovat chyby
                    }
                }
            }

            weightWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val reps = binding.repsInput.text.toString().toIntOrNull() ?: 0
                        val weight = s.toString().toFloatOrNull() ?: 0f
                        onSetChanged(currentPosition, reps, weight)
                    } catch (e: Exception) {
                        // Ignorovat chyby
                    }
                }
            }

            binding.repsInput.addTextChangedListener(repsWatcher)
            binding.weightInput.addTextChangedListener(weightWatcher)

            binding.completeButton.setOnClickListener {
                onSetCompleted(currentPosition)
                // Aktualizovat pouze tuto polozku pro zobrazeni zelene barvy
                notifyItemChanged(currentPosition)
            }
        }
    }
}
