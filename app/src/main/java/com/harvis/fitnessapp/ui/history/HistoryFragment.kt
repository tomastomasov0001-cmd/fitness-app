package com.harvis.fitnessapp.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { item ->
            // Navigate to workout fragment to view/edit completed workout
            val bundle = Bundle().apply {
                putLong("variantId", item.log.variantId)
                putLong("workoutLogId", item.log.id)
                putBoolean("viewOnly", true)
            }
            findNavController().navigate(R.id.action_history_to_workout, bundle)
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter
    }

    private fun observeHistory() {
        viewModel.allWorkoutLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.emptyView.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
