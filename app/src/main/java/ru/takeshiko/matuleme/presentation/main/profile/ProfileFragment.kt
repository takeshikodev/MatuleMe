package ru.takeshiko.matuleme.presentation.main.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import ru.takeshiko.matuleme.R
import ru.takeshiko.matuleme.data.remote.SupabaseClientManager
import ru.takeshiko.matuleme.data.utils.MaterialToast
import ru.takeshiko.matuleme.databinding.FragmentProfileBinding
import ru.takeshiko.matuleme.domain.models.result.DataResult
import ru.takeshiko.matuleme.presentation.deliveryaddresses.DeliveryAddressesActivity
import ru.takeshiko.matuleme.presentation.paymentcards.PaymentCardsActivity

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var toast: MaterialToast
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        toast = MaterialToast(requireContext())

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { uploadAvatar(it) }
        }

        val factory = ProfileViewModelFactory(SupabaseClientManager.getInstance())
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        with (binding) {
            tvChangeAvatar.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            viewModel.userResult.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is DataResult.Success -> {
                        val userMetadata = result.data.userMetadata
                        if (userMetadata != null) {
                            val firstName = userMetadata["first_name"]?.toString()?.trim('"') ?: ""
                            val lastName = userMetadata["last_name"]?.toString()?.trim('"') ?: ""
                            tvFullName.text = if (firstName.isEmpty() && lastName.isEmpty()) {
                                getString(R.string.full_name_prompt, getString(R.string.no_data), "")
                            } else {
                                getString(R.string.full_name_prompt, firstName, lastName)
                            }

                            etFirstName.setText(firstName)
                            etLastName.setText(lastName)

                            val phone = userMetadata["phone_number"]?.toString()?.trim('"') ?: ""
                            etPhone.setText(phone)

                            val avatarUrl = viewModel.getAvatarFromUrlDefault(result.data.id)
                            Glide
                                .with(requireContext())
                                .load("$avatarUrl?timestamp=${System.currentTimeMillis()}")
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .centerCrop()
                                .into(ivAvatar)
                        }
                    }
                    is DataResult.Error -> {
                        toast.show(
                            getString(R.string.failed_title),
                            result.message,
                            R.drawable.ic_cross
                        )
                        Log.d(javaClass.name, result.message)
                    }
                }
            }

            viewModel.defaultAddress.observe(viewLifecycleOwner) { address ->
                cardAddress.etAddress.setText(address)
                cardAddress.tilAddress.startIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker)
            }

            viewModel.defaultPaymentCard.observe(viewLifecycleOwner) { paymentCard ->
                cardPaymentCard.etPaymentCard.setText(paymentCard)
                cardPaymentCard.tilPaymentCard.startIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_visa_card)
            }

            tilFirstName.setEndIconOnClickListener {
                val firstName = etFirstName.text.toString().trim()
                if (firstName.isNotEmpty()) {
                    viewModel.updateUserFirstName(firstName = firstName)
                    toast.show(
                        getString(R.string.edit_profile_success_title),
                        getString(R.string.edit_profile_success_message),
                        R.drawable.ic_checkmark
                    )
                } else {
                    toast.show(
                        getString(R.string.edit_profile_failed_title),
                        getString(R.string.edit_profile_failed_message),
                        R.drawable.ic_cross
                    )
                }
            }

            tilLastName.setEndIconOnClickListener {
                val lastName = etLastName.text.toString().trim()
                if (lastName.isNotEmpty()) {
                    viewModel.updateUserLastName(lastName = lastName)
                    toast.show(
                        getString(R.string.edit_profile_success_title),
                        getString(R.string.edit_profile_success_message),
                        R.drawable.ic_checkmark
                    )
                } else {
                    toast.show(
                        getString(R.string.edit_profile_failed_title),
                        getString(R.string.edit_profile_failed_message),
                        R.drawable.ic_cross
                    )
                }
            }

            tilPhone.setEndIconOnClickListener {
                val phone = etPhone.text.toString().trim()
                if (phone.isNotEmpty()) {
                    viewModel.updateUserPhoneNumber(phoneNumber = phone)
                    toast.show(
                        getString(R.string.edit_profile_success_title),
                        getString(R.string.edit_profile_success_message),
                        R.drawable.ic_checkmark
                    )
                } else {
                    toast.show(
                        getString(R.string.edit_profile_failed_title),
                        getString(R.string.edit_profile_failed_message),
                        R.drawable.ic_cross
                    )
                }
            }

            cardAddress.tilAddress.setEndIconOnClickListener {
                startActivity(Intent(requireContext(), DeliveryAddressesActivity::class.java))
            }

            cardAddress.ivMap.setOnClickListener {
                val address = cardAddress.etAddress.text.toString().trim()
                if (address.isNotEmpty()) {
                    openAddressInMaps(address)
                }
            }

            cardPaymentCard.tilPaymentCard.setEndIconOnClickListener {
                startActivity(Intent(requireContext(), PaymentCardsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
        viewModel.loadUserDefaultAddress()
        viewModel.loadUserDefaultPaymentCard()
    }

    private fun openAddressInMaps(address: String) {
        val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")

        val googleMapsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.google.com/maps/search/?api=1&query=$encodedAddress".toUri()
        }

        val yandexMapsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "yandexmaps://maps.yandex.ru/?text=$encodedAddress".toUri()
        }

        val dgisIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "dgis://2gis.ru/query=$encodedAddress".toUri()
        }

        val chooser = Intent.createChooser(googleMapsIntent, getString(R.string.open_address_in)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(yandexMapsIntent, dgisIntent))
        }

        try {
            startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            toast.show(
                getString(R.string.failed_title),
                getString(R.string.install_app_maps),
                R.drawable.ic_cross
            )
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
        inputStream.close()

        viewModel.uploadAvatar(requireContext(), uri)
        toast.show(
            getString(R.string.edit_profile_success_title),
            getString(R.string.edit_profile_success_message),
            R.drawable.ic_checkmark
        )
    }
}