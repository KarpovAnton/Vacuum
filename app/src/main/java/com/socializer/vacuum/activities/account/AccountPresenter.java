package com.socializer.vacuum.activities.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.socializer.vacuum.VacuumApplication;
import com.socializer.vacuum.di.base.ActivityScoped;
import com.socializer.vacuum.network.data.DtoCallback;
import com.socializer.vacuum.network.data.DtoListCallback;
import com.socializer.vacuum.network.data.FailTypes;
import com.socializer.vacuum.network.data.dto.ProfilePreviewDto;
import com.socializer.vacuum.network.data.dto.ProfilePreviewDto.ProfileAccountDto;
import com.socializer.vacuum.network.data.dto.ProfilePreviewDto.ProfileImageDto;
import com.socializer.vacuum.network.data.dto.ResponseDto;
import com.socializer.vacuum.network.data.managers.LoginManager;
import com.socializer.vacuum.network.data.managers.ProfilesManager;
import com.socializer.vacuum.network.data.prefs.AuthSession;
import com.socializer.vacuum.services.BleManager;
import com.socializer.vacuum.utils.StringPreference;
import com.socializer.vacuum.views.adapters.PhotosAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.socializer.vacuum.activities.account.AccountActivity.FB;
import static com.socializer.vacuum.activities.account.AccountActivity.INST;
import static com.socializer.vacuum.activities.account.AccountActivity.VK;
import static com.socializer.vacuum.network.data.prefs.PrefsModule.NAMED_PREF_DEVICE_NAME;
import static com.socializer.vacuum.network.data.prefs.PrefsModule.NAMED_PREF_SOCIAL;
import static com.socializer.vacuum.utils.Consts.BASE_DEVICE_NAME_PART;

@ActivityScoped
public class AccountPresenter implements AccountContract.Presenter {

    public static final String VK_BASE_URL = "https://www.vk.com/profile.php?id=";
    public static final String FB_BASE_URL = "https://www.facebook.com/profile.php?id=";
    public static final String FB_HOST_URL = "https://www.facebook.com/";
    public static final String INST_BASE_URL = "https://www.instagram.com/";

    @Inject
    @Named(NAMED_PREF_DEVICE_NAME)
    StringPreference deviceNameSP;

    @Inject
    @Named(NAMED_PREF_SOCIAL)
    StringPreference socialSP;

    PhotosAdapter adapter;
    ProfilePreviewDto currentAccountDto;

    @Nullable
    AccountContract.View view;

    @Inject
    AccountRouter router;

    @Inject
    LoginManager loginManager;

    @Inject
    BleManager bleManager;

    @Inject
    public AccountPresenter() {
        adapter = new PhotosAdapter(VacuumApplication.applicationContext);
    }

    @Inject
    ProfilesManager profilesManager;

    @Override
    public void takeView(AccountContract.View view) {
        this.view = view;
        this.view.setAdapter(adapter);
    }

    @Override
    public void dropView() {
        this.view = null;
    }

    @Override
    public void loadAccount(String profileId) {
        profilesManager.getProfile(profileId, new DtoListCallback<ResponseDto>() {
            @Override
            public void onSuccessful(@NonNull List<ProfilePreviewDto> response) {
                if (!response.isEmpty()) {
                    currentAccountDto = response.get(0);

                    List<ProfileImageDto> photos = currentAccountDto.getImages();
                    if (photos != null) {
                        adapter.setPhotos(photos);
                    }

                    view.onAccountLoaded(currentAccountDto);
                }
            }

            @Override
            public void onFailed(FailTypes fail) {
                if (view != null)
                    view.showErrorNetworkDialog(fail);
            }
        });
    }

    @Override
    public void bindSocial(int kind, String socialUserId, String accessToken, @Nullable String username) {
        String baseUrl;
        switch (kind) {
            case VK:
                baseUrl = VK_BASE_URL;
                break;
            case FB:
                baseUrl = FB_BASE_URL;
                break;
            case INST:
                baseUrl = INST_BASE_URL;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }

        String url;
        if (kind != INST) {
            url = baseUrl + socialUserId;
        } else {
            url = baseUrl + username;
        }

        loginManager.bindSocial(
                kind,
                url,
                socialUserId,
                accessToken,
                new DtoCallback<ResponseDto>() {
                    @Override
                    public void onSuccessful(@NonNull ResponseDto response) {
                        AuthSession.getInstance().setToken(accessToken);
                        ProfilePreviewDto result = (ProfilePreviewDto) response;
                        String newId = result.getUserId();
                        String deviceName = newId + BASE_DEVICE_NAME_PART;
                        deviceNameSP.set(deviceName);
                        bleManager.setBluetoothAdapterName(deviceName);
                        socialSP.set("true");
                        if (view != null)
                            view.onSocialBinded();
                    }

                    @Override
                    public void onFailed(FailTypes fail) {
                        if (view != null)
                            view.showErrorNetworkDialog(fail);
                    }
                });
    }

    @Override
    public void unBindSocial(int kind) {
        loginManager.unBindSocial(kind, new DtoCallback<ResponseDto>() {
            @Override
            public void onSuccessful(@NonNull ResponseDto response) {
                checkIfAnySocialBinded();
                if (view != null)
                    view.onSocUnBind(kind);
            }

            @Override
            public void onFailed(FailTypes fail) {
                if (view != null)
                    view.showErrorNetworkDialog(fail);
            }
        });
    }

    private void checkIfAnySocialBinded() {
        String profileId = deviceNameSP.get();
        if (profileId != null) {
            profilesManager.getProfile(profileId, new DtoListCallback<ResponseDto>() {
                @Override
                public void onSuccessful(@NonNull List<ProfilePreviewDto> response) {
                    if (!response.isEmpty()) {
                        currentAccountDto = response.get(0);
                        List<ProfileAccountDto> accounts = currentAccountDto.getAccounts();
                        for (int i = 0; i < accounts.size(); i++) {
                            ProfileAccountDto acc = accounts.get(i);
                            if (acc.getKind() == VK || acc.getKind() == FB || acc.getKind() == INST) {
                                socialSP.set("true");
                            } else {
                                socialSP.set("false");
                            }
                        }
                    }
                }

                @Override
                public void onFailed(FailTypes fail) {
                    if (view != null)
                        view.showErrorNetworkDialog(fail);
                }
            });
        }
    }

    @Override
    public void openVKProfile() {
        List<ProfileAccountDto> accounts = currentAccountDto.getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            ProfileAccountDto acc = accounts.get(i);
            if (acc.getKind() == VK) {
                String url = acc.getUrl();
                String profileId = url.split("=")[1];
                router.openVKProfile(profileId);
                return;
            }
        }
    }

    @Override
    public void openFBProfile() {
        List<ProfileAccountDto> accounts = currentAccountDto.getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            ProfileAccountDto acc = accounts.get(i);
            if (acc.getKind() == FB) {
                String url = acc.getUrl();
                String profileId = url.split("=")[1];
                router.openFBProfile(profileId);
                return;
            }
        }
    }

    @Override
    public void openInstProfile() {
        List<ProfileAccountDto> accounts = currentAccountDto.getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            ProfileAccountDto acc = accounts.get(i);
            if (acc.getKind() == INST) {
                String url = acc.getUrl();
                String profileId = url.split("com/")[1];
                router.openINSTProfile(profileId);
                return;
            }
        }
    }
}