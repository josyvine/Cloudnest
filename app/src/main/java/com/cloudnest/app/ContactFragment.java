package com.cloudnest.app;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.cloudnest.app.databinding.FragmentContactBinding;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Contact Support Page.
 * Allows users to send feedback or report bugs directly to the developer.
 * Uses Formspree API (https://formspree.io/f/xyzenlao) via Retrofit.
 */
public class ContactFragment extends Fragment {

    private FragmentContactBinding binding;
    private FormspreeService apiService;
    private ProgressDialog progressDialog;

    // API Configuration
    private static final String BASE_URL = "https://formspree.io/";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view);
        setupRetrofit();
        setupSendButton();
    }

    /**
     * Configures the top toolbar with a Back button.
     */
    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar_contact);
        if (toolbar != null) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Contact Support");
            }
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }
    }

    /**
     * Initializes the Retrofit client for network requests.
     */
    private void setupRetrofit() {
        // Logging for debugging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(FormspreeService.class);
    }

    /**
     * Handles the Send button click event.
     */
    private void setupSendButton() {
        binding.btnSendMessage.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String message = binding.etMessage.getText().toString().trim();

            if (validateInputs(name, email, message)) {
                sendEmail(name, email, message);
            }
        });
    }

    /**
     * Validates user input before sending.
     */
    private boolean validateInputs(String name, String email, String message) {
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email is required");
            return false;
        }
        if (TextUtils.isEmpty(message)) {
            binding.etMessage.setError("Message cannot be empty");
            return false;
        }
        return true;
    }

    /**
     * Executes the network request to Formspree.
     */
    private void sendEmail(String name, String email, String message) {
        // Show loading state
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Sending message...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Prepare request body
        ContactRequest request = new ContactRequest(name, email, message);

        // Make call
        Call<Void> call = apiService.sendMessage(request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_LONG).show();
                    clearFields();
                    // Optional: Navigate back after success
                    // Navigation.findNavController(binding.getRoot()).navigateUp();
                } else {
                    Toast.makeText(requireContext(), "Failed to send. Server error.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearFields() {
        binding.etName.setText("");
        binding.etEmail.setText("");
        binding.etMessage.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Internal Retrofit Interface ---

    public interface FormspreeService {
        @POST("f/xyzenlao")
        Call<Void> sendMessage(@Body ContactRequest body);
    }

    // --- Internal Data Model ---

    public static class ContactRequest {
        @SerializedName("name")
        String name;

        @SerializedName("email")
        String email;

        @SerializedName("message")
        String message;

        public ContactRequest(String name, String email, String message) {
            this.name = name;
            this.email = email;
            this.message = message;
        }
    }
}