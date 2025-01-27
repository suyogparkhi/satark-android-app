package org.cyducks.satark.dashboard.civilian.ui;



import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.cyducks.satark.AuthActivity;
import org.cyducks.satark.core.MapConstants;
import org.cyducks.satark.core.heatmap.HeatMapManager;
import org.cyducks.satark.core.heatmap.domain.repository.HeatMapRepository;
import org.cyducks.satark.core.heatmap.domain.viewmodel.SettingsViewModel;
import org.cyducks.satark.core.safetyscore.domain.SafetyScoreViewModel;
import org.cyducks.satark.dashboard.viewmodel.DashboardViewModel;
import org.cyducks.satark.databinding.FragmentHomeDriverBinding;
import org.cyducks.satark.util.LocationUtil;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.CompositeDisposable;



public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "CivilianHomeFragment";
    FragmentHomeDriverBinding viewBinding;
    DashboardViewModel dashboardViewModel;
    SafetyScoreViewModel safetyScoreViewModel;
    private HeatMapRepository heatMapRepository;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private static final int REPORT_COOLDOWN_PERIOD_MS = 10000;
    WorkManager workManager;


    public HomeFragment() {
        // Required empty public constructor
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String city = prefs.getString("user_city", "Unknown");

        try {
            heatMapRepository = new HeatMapRepository(requireContext(), city);
        } catch (UnsupportedOperationException e) {
            heatMapRepository = null;
        }
        workManager = WorkManager.getInstance(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewBinding = FragmentHomeDriverBinding.inflate(getLayoutInflater());
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        safetyScoreViewModel = new ViewModelProvider(requireActivity()).get(SafetyScoreViewModel.class);

        ObjectAnimator animator = ObjectAnimator.ofFloat(viewBinding.refreshButton, View.ROTATION, 0f, 360f)
                .setDuration(1000);

        safetyScoreViewModel.getSafetyScore().observe(getViewLifecycleOwner(), score -> {
            viewBinding.safetyScoreText.setText(String.valueOf(score));
            viewBinding.safetyScoreText.setTextColor(safetyScoreViewModel.getScoreColor(score));
            viewBinding.safetyScoreText.setVisibility(View.VISIBLE);
        });

        safetyScoreViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            viewBinding.scoreLoadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            viewBinding.safetyScoreText.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            viewBinding.refreshButton.setEnabled(!isLoading);

            if(isLoading) {


                animator.setInterpolator(new LinearInterpolator());
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.start();

            } else {
                viewBinding.refreshButton.animate().cancel();
                viewBinding.refreshButton.setRotation(0f);
                animator.pause();
            }
        });

        safetyScoreViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if(error != null) {
                Snackbar.make(viewBinding.safetyScoreCard, error, BaseTransientBottomBar.LENGTH_LONG)
                        .setAction("Retry", v -> safetyScoreViewModel.refreshScore())
                        .show();
            }
        });


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

//        viewBinding.liveReportButton.setOnClickListener(v -> {
//            GrpcRunnable runnable = new ReportStreamRunnable("H7trkTwIVzOHMKYLryOgIkZ8vn23", new StreamObserver<Report>() {
//                @Override
//                public void onNext(Report value) {
//
//                }
//
//                @Override
//                public void onError(Throwable t) {
//
//                }
//
//                @Override
//                public void onCompleted() {
//
//                }
//            });
//            ManagedChannel channel = ManagedChannelBuilder.forAddress("10.0.2.2", 9000).usePlaintext().build();
//
//            try {
//                runnable.run(ReportServiceGrpc.newBlockingStub(channel), ReportServiceGrpc.newStub(channel));
//            } catch (Exception e) {
//                Log.d(TAG, "onCreateView: " + e);
//            }
//        });
//
//
//        viewBinding.reportButton.setOnClickListener(v -> {
//
//            Random random = new Random();
//
//
//            // Latitude range for Nagpur
//            float minLatitude = 21.0725f;
//            float maxLatitude = 21.2050f;
//
//            // Longitude range for Nagpur
//            float minLongitude = 79.0020f;
//            float maxLongitude = 79.1690f;
//
//
//            // Generate random latitude and longitude within the specified range
//            float latitude = minLatitude + (maxLatitude - minLatitude) * random.nextFloat();
//            float longitude = minLongitude + (maxLongitude - minLongitude) * random.nextFloat();
//
//
//
//            assert user != null;
//            OneTimeWorkRequest sendReportRequest = new OneTimeWorkRequest.Builder(GrpcWorker.class)
//                    .setInputData(new Data.Builder()
//                            .putString("request_type", "sendReport")
//                            .putString("zone_id", "A")
//                            .putString("mod_id", "H7trkTwIVzOHMKYLryOgIkZ8vn23")
//                            .putFloat("lat", latitude)
//                            .putFloat("lng", longitude)
//                            .putString("type", "dange")
//                            .build())
//                    .build();
//            ;
//
//
//            workManager
//                    .enqueue(sendReportRequest);
//
//
//            workManager
//                    .getWorkInfoByIdLiveData(sendReportRequest.getId())
//                    .observe(getViewLifecycleOwner(), workInfo -> {
//                        if(workInfo.getState().isFinished() && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                            Toast.makeText(requireContext(), "Report Sent", Toast.LENGTH_SHORT).show();
//                            Log.d("MYAPP", "onCreateView: " + workInfo.getOutputData());
//                        } else if(workInfo.getState().isFinished() && workInfo.getState() == WorkInfo.State.FAILED) {
//                            Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
//                            Log.d("MYAPP", "onCreateView: " + workInfo.getOutputData());
//                        }
//                    });
//        });

        // Inflate the layout for this fragment
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SupportMapFragment mapFragment = viewBinding.map.getFragment();
        mapFragment.getMapAsync(this);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        // Setup HeatMap
        if(heatMapRepository != null) {
            HeatMapManager mapManager = new HeatMapManager(requireContext(), googleMap, heatMapRepository);
            settingsViewModel.setHeatMapManager(mapManager);
        }

        final AtomicBoolean simulationMode = new AtomicBoolean(false);

        LatLng center = googleMap.getCameraPosition().target;
        safetyScoreViewModel.fetchSafetyScore(center.latitude, center.longitude);

        OptionsSheetFragment fragment = new OptionsSheetFragment();

        viewBinding.optionButton.setOnClickListener(v -> {
            fragment.show(getChildFragmentManager(), "TAG");
        });

        viewBinding.refreshButton.setOnClickListener(v -> {
            LatLng cent = googleMap.getCameraPosition().target;
            Log.d(TAG, "onMapReady: " + cent);
            safetyScoreViewModel.fetchSafetyScore(cent.latitude, cent.longitude);
        });

        viewBinding.fabReport.setOnClickListener(v -> {
            if (!simulationMode.get()) {
                if(cooldownEnabled()) {
                    showWarning();
                    return;
                }
                saveClickTimestamp(System.currentTimeMillis());

            }
            ReportSheetFragment reportSheetFragment = new ReportSheetFragment();
            reportSheetFragment.show(getChildFragmentManager(), "DangerReport");
        });

        viewBinding.signoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        settingsViewModel.getOperationMode().observe(getViewLifecycleOwner(), mode -> {
            if(mode != simulationMode.get()) {
                setMapLocation(mode, googleMap);
            }
            simulationMode.set(mode);
        });

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(21.1458, 79.0882), 15f));
    }

    private void updateButtonState() {
        viewBinding.fabReport.setEnabled(!cooldownEnabled());
    }

    private void saveClickTimestamp(long time) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("last_click_time", String.valueOf(time)).apply();
    }

    private boolean cooldownEnabled() {
        long lastClickTime = getLastClickTime();
        return System.currentTimeMillis() - lastClickTime < REPORT_COOLDOWN_PERIOD_MS;
    }

    private long getLastClickTime() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String lastClickTime = prefs.getString("last_click_time", null);

        if(lastClickTime == null) {
            return 0;
        }

        return Long.parseLong(lastClickTime);
    }

    private void showWarning() {
        long timeLeft = (getLastClickTime() + REPORT_COOLDOWN_PERIOD_MS - System.currentTimeMillis()) / 1000;
        Toast.makeText(requireContext(),
                "Please wait " + timeLeft + " seconds before trying again",
                Toast.LENGTH_SHORT).show();
    }

    private void setMapLocation(Boolean simulationMode, GoogleMap googleMap) {


        if(simulationMode) {
            LatLng mapLocation;
            SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            if(!prefs.contains("user_city")) {
                throw new IllegalStateException("user city not specified");
            }


            String city = prefs.getString("user_city", "Nagpur");
            switch (city) {
                case "Mumbai":
                    mapLocation = MapConstants.MUMBAI_CENTER;
                    break;
                case "Delhi":
                    mapLocation = MapConstants.DELHI_CENTER;
                    break;
                case "Pune":
                    mapLocation = MapConstants.PUNE_CENTER;
                    break;
                case "Nagpur":
                default:
                    mapLocation = MapConstants.NAGPUR_CENTER;
                    break;
            }

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapLocation, MapConstants.INITIAL_ZOOM));

        } else {
            LocationUtil locationUtil = new LocationUtil(requireActivity());
            locationUtil.getCurrentLocation(new LocationUtil.LocationListener() {
                @Override
                public void onResult(Location location) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), MapConstants.INITIAL_ZOOM));
                }

                @Override
                public void onError(String exception) {
                    Toast.makeText(requireContext(), exception, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
        heatMapRepository.cleanup();
    }
}