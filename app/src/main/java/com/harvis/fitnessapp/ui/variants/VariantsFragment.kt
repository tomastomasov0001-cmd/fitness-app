package com.harvis.fitnessapp.ui.variants

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.VariantWithCount
import com.harvis.fitnessapp.data.WorkoutVariant
import com.harvis.fitnessapp.databinding.FragmentVariantsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VariantsFragment : Fragment() {

    private var _binding: FragmentVariantsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VariantsViewModel
    private lateinit var adapter: VariantsAdapter

    // Launcher pro ulozeni exportu
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val json = viewModel.exportResult.value
            if (json != null) {
                try {
                    requireContext().contentResolver.openOutputStream(it)?.use { output ->
                        output.write(json.toByteArray())
                    }
                    Toast.makeText(requireContext(), "Export dokončen", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Chyba při ukládání", Toast.LENGTH_SHORT).show()
                }
            }
            viewModel.clearExportResult()
        }
    }

    // Launcher pro vyber souboru k importu
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.openInputStream(it)?.use { input ->
                    val json = input.bufferedReader().readText()
                    viewModel.importData(json)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Chyba při čtení souboru", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVariantsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[VariantsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        setupExportImport()
        observeVariants()
        observeExportImport()
    }

    private fun setupRecyclerView() {
        adapter = VariantsAdapter(
            onItemClick = { variant ->
                val bundle = bundleOf("variantId" to variant.id)
                findNavController().navigate(R.id.action_variants_to_variantDetail, bundle)
            },
            onEditClick = { variant ->
                showEditVariantDialog(variant)
            },
            onDeleteClick = { variant ->
                showDeleteDialog(variant.id, variant.name)
            }
        )
        binding.variantsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.variantsRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddVariant.setOnClickListener {
            showAddVariantDialog()
        }
    }

    private fun setupExportImport() {
        binding.exportButton.setOnClickListener {
            viewModel.exportData()
        }

        binding.importButton.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
        }
    }

    private fun observeExportImport() {
        viewModel.exportResult.observe(viewLifecycleOwner) { json ->
            if (json != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                val fileName = "fitness_backup_${dateFormat.format(Date())}.json"
                exportLauncher.launch(fileName)
            }
        }

        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.success) {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                }
                viewModel.clearImportResult()
            }
        }
    }

    private fun observeVariants() {
        viewModel.allVariantsWithCount.observe(viewLifecycleOwner) { variants ->
            adapter.submitList(variants)
            binding.emptyView.visibility = if (variants.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddVariantDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_variant, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.variantNameInput)
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.variantDescInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_variant)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val variant = WorkoutVariant(
                        name = name,
                        description = descInput.text.toString().trim()
                    )
                    viewModel.insertVariant(variant)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditVariantDialog(variant: VariantWithCount) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_variant, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.variantNameInput)
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.variantDescInput)

        // Pre-fill current values
        nameInput.setText(variant.name)
        descInput.setText(variant.description)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updatedVariant = WorkoutVariant(
                        id = variant.id,
                        name = name,
                        description = descInput.text.toString().trim()
                    )
                    viewModel.updateVariant(updatedVariant)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(variantId: Long, variantName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("Opravdu smazat '$variantName'?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteVariantById(variantId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
