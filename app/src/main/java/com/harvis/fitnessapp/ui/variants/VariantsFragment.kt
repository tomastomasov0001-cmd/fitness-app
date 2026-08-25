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
import com.harvis.fitnessapp.util.PremiumDialogHelper
import com.harvis.fitnessapp.util.PremiumManager
import com.harvis.fitnessapp.databinding.FragmentVariantsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VariantsFragment : Fragment() {

    private var _binding: FragmentVariantsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VariantsViewModel
    private lateinit var adapter: VariantsAdapter
    private var currentVariantCount = 0

    // Debug mode
    private var debugTapCount = 0
    private var lastDebugTapTime = 0L
    private val debugPassword = "1196"  // Tajné heslo pro debug menu

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
                    Toast.makeText(requireContext(), getString(R.string.export_completed), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.error_saving), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), getString(R.string.error_reading_file), Toast.LENGTH_SHORT).show()
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
        setupDebugMode()
        observeVariants()
        observeExportImport()
    }

    private fun setupDebugMode() {
        // 7x tap na nadpis otevře debug menu (s heslem)
        binding.title.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastDebugTapTime > 2000) {
                debugTapCount = 0
            }
            lastDebugTapTime = now
            debugTapCount++

            if (debugTapCount >= 7) {
                debugTapCount = 0
                showDebugPasswordDialog()
            }
        }

        // Dlouhé podržení také otevře debug menu (s heslem)
        binding.title.setOnLongClickListener {
            showDebugPasswordDialog()
            true
        }
    }

    private fun showDebugPasswordDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Heslo"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Developer Access")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                if (input.text.toString() == debugPassword) {
                    showDebugMenu()
                } else {
                    Toast.makeText(requireContext(), "Špatné heslo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun showDebugMenu() {
        val context = requireContext()
        val premiumType = PremiumManager.getPremiumType(context)
        val remainingDays = PremiumManager.getPromoRemainingDays(context)

        val statusText = when (premiumType) {
            PremiumManager.PremiumType.PURCHASED -> "PREMIUM (zakoupeno)"
            PremiumManager.PremiumType.PROMO -> "TRIAL ($remainingDays dní zbývá)"
            PremiumManager.PremiumType.NONE -> "FREE"
        }

        val options = arrayOf(
            "Přepnout Premium (aktuálně: $statusText)",
            "Resetovat promo kódy",
            "Zadat promo kód",
            "Generovat VIP kód pro přítele",
            "Zobrazit info"
        )

        MaterialAlertDialogBuilder(context)
            .setTitle("Debug Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val newState = PremiumManager.debugTogglePremium(context)
                        val msg = if (newState) "Premium ZAPNUTO" else "Premium VYPNUTO"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        PremiumManager.debugResetAllPromoCodes(context)
                        Toast.makeText(context, "Promo kódy resetovány", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        PremiumDialogHelper.showPromoCodeDialog(requireActivity())
                    }
                    3 -> {
                        showGenerateVipCodeDialog()
                    }
                    4 -> {
                        showDebugInfo()
                    }
                }
            }
            .setNegativeButton("Zavřít", null)
            .show()
    }

    private fun showGenerateVipCodeDialog() {
        val codes = mutableListOf<String>()
        repeat(5) {
            codes.add(PremiumManager.generateVipCode())
        }

        val message = buildString {
            appendLine("Vygenerované VIP kódy (trvalý premium):")
            appendLine()
            codes.forEach { code ->
                appendLine("  $code")
            }
            appendLine()
            appendLine("Každý kód lze použít pouze JEDNOU.")
            appendLine("Zkopíruj a pošli příteli.")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("VIP kódy")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Kopírovat první") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("VIP kód", codes.first())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Zkopírováno: ${codes.first()}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDebugInfo() {
        val context = requireContext()
        val premiumType = PremiumManager.getPremiumType(context)
        val remainingDays = PremiumManager.getPromoRemainingDays(context)
        val activeCode = PremiumManager.getActivePromoCode(context)

        val info = buildString {
            appendLine("=== Premium Info ===")
            appendLine("Typ: $premiumType")
            appendLine("Zakoupeno: ${PremiumManager.isPremiumPurchased(context)}")
            appendLine("Promo aktivní: ${PremiumManager.isPromoActive(context)}")
            if (remainingDays >= 0) {
                appendLine("Zbývá dní: $remainingDays")
            }
            if (activeCode != null) {
                appendLine("Aktivní kód: $activeCode")
            }
            appendLine()
            appendLine("=== Limity ===")
            appendLine("Max variant (free): ${PremiumManager.FREE_MAX_VARIANTS}")
            appendLine("Max cviků (free): ${PremiumManager.FREE_MAX_EXERCISES}")
            appendLine("Aktuální variant: $currentVariantCount")
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Debug Info")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
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
            if (PremiumManager.canAddVariant(requireContext(), currentVariantCount)) {
                showAddVariantDialog()
            } else {
                PremiumDialogHelper.showVariantLimitDialog(requireActivity())
            }
        }
    }

    private fun setupExportImport() {
        binding.exercisesButton.setOnClickListener {
            findNavController().navigate(R.id.action_variants_to_exercises)
        }

        binding.exercisesCard.setOnClickListener {
            findNavController().navigate(R.id.action_variants_to_exercises)
        }

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
            currentVariantCount = variants.size
            adapter.submitList(variants)
            binding.emptyView.visibility = if (variants.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.allExercises.observe(viewLifecycleOwner) { exercises ->
            val count = exercises.size
            binding.exercisesCount.text = when (count) {
                0 -> getString(R.string.exercise_count_zero)
                1 -> getString(R.string.exercise_count_one)
                else -> getString(R.string.exercise_count_many, count)
            }
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
                    // Zachovat puvodni createdAt timestamp
                    val updatedVariant = WorkoutVariant(
                        id = variant.id,
                        name = name,
                        description = descInput.text.toString().trim(),
                        createdAt = variant.createdAt
                    )
                    viewModel.updateVariant(updatedVariant)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(variantId: Long, variantName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_warning_title)
            .setMessage(getString(R.string.delete_variant_warning, variantName))
            .setPositiveButton(R.string.delete_anyway) { _, _ ->
                viewModel.deleteVariantById(variantId)
            }
            .setNeutralButton(R.string.export_first) { _, _ ->
                // Spustit export před smazáním
                viewModel.exportData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
