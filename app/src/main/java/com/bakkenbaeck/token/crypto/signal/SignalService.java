package com.bakkenbaeck.token.crypto.signal;


import com.bakkenbaeck.token.R;
import com.bakkenbaeck.token.crypto.HDWallet;
import com.bakkenbaeck.token.crypto.signal.model.SignalBootstrap;
import com.bakkenbaeck.token.crypto.signal.network.SignalInterface;
import com.bakkenbaeck.token.crypto.signal.store.ProtocolStore;
import com.bakkenbaeck.token.model.network.ServerTime;
import com.bakkenbaeck.token.network.interceptor.LoggingInterceptor;
import com.bakkenbaeck.token.network.interceptor.SigningInterceptor;
import com.bakkenbaeck.token.network.interceptor.UserAgentInterceptor;
import com.bakkenbaeck.token.view.BaseApplication;
import com.squareup.moshi.Moshi;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.push.PreKeyEntity;
import org.whispersystems.signalservice.internal.util.JsonUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import rx.SingleSubscriber;
import rx.schedulers.Schedulers;

public final class SignalService extends SignalServiceAccountManager {

    private static final String PREKEY_PATH = "/v1/accounts/bootstrap/";
    private final HDWallet wallet;
    private final SignalInterface signalInterface;
    private final OkHttpClient.Builder client;
    private final String url;

    public SignalService(
            final TrustStore trustStore,
            final HDWallet wallet,
            final ProtocolStore protocolStore,
            final String userAgent) {
        this(BaseApplication.get().getResources().getString(R.string.chat_url),
                trustStore,
                wallet,
                wallet.getOwnerAddress(),
                protocolStore.getPassword(),
                userAgent);
    }

    private SignalService(final String url,
                          final TrustStore trustStore,
                          final HDWallet wallet,
                          final String user,
                          final String password,
                          final String userAgent) {
        super(url, trustStore, user, password, userAgent);
        this.wallet = wallet;
        this.url = url;
        this.client = new OkHttpClient.Builder();
        this.signalInterface = generateSignalInterface();
    }

    private SignalInterface generateSignalInterface() {
        final RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());

        addUserAgentHeader();
        addSigningInterceptor();
        addLogging();

        final Moshi moshi = new Moshi.Builder()
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(this.url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(rxAdapter)
                .client(client.build())
                .build();
        return retrofit.create(SignalInterface.class);
    }

    private void addUserAgentHeader() {
        this.client.addInterceptor(new UserAgentInterceptor());
    }

    private void addSigningInterceptor() {
        this.client.addInterceptor(new SigningInterceptor());
    }

    private void addLogging() {
        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new LoggingInterceptor());
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        this.client.addInterceptor(interceptor);
    }

    public void registerKeys(final ProtocolStore protocolStore, final SingleSubscriber<Void> registrationSubscriber) {
        try {
            registerKeys(
                    protocolStore.getIdentityKeyPair().getPublicKey(),
                    protocolStore.getLastResortKey(),
                    protocolStore.getPassword(),
                    protocolStore.getLocalRegistrationId(),
                    protocolStore.getSignalingKey(),
                    protocolStore.getSignedPreKey(),
                    protocolStore.getPreKeys(),
                    registrationSubscriber
            );
        } catch (final IOException | InvalidKeyIdException ex) {
            registrationSubscriber.onError(ex);
        }
    }

    private void registerKeys(
            final IdentityKey identityKey,
            final PreKeyRecord lastResortKey,
            final String password,
            final int registrationId,
            final String signalingKey,
            final SignedPreKeyRecord signedPreKey,
            final List<PreKeyRecord> preKeys,
            final SingleSubscriber<Void> registrationSubscriber) {

        this.signalInterface.getTimestamp()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleSubscriber<ServerTime>() {
                    @Override
                    public void onSuccess(final ServerTime serverTime) {
                        try {
                            registerKeysWithTimestamp(
                                    serverTime.get(),
                                    identityKey,
                                    lastResortKey,
                                    password,
                                    registrationId,
                                    signalingKey,
                                    signedPreKey,
                                    preKeys,
                                    registrationSubscriber);
                        } catch (final IOException ex) {
                            registrationSubscriber.onError(ex);
                        }
                    }

                    @Override
                    public void onError(final Throwable throwable) {
                        registrationSubscriber.onError(throwable);
                    }
                });
    }

    private void registerKeysWithTimestamp(
            final long timestamp,
            final IdentityKey identityKey,
            final PreKeyRecord lastResortKey,
            final String password,
            final int registrationId,
            final String signalingKey,
            final SignedPreKeyRecord signedPreKey,
            final List<PreKeyRecord> preKeys,
            final SingleSubscriber<Void> registrationSubscriber) throws IOException {

        final long startTime = System.currentTimeMillis();

        final List<PreKeyEntity> entities = new LinkedList<>();
        for (PreKeyRecord preKey : preKeys) {
            final PreKeyEntity entity = new PreKeyEntity(
                    preKey.getId(),
                    preKey.getKeyPair().getPublicKey());
            entities.add(entity);
        }

        final PreKeyEntity lastResortEntity = new PreKeyEntity(
                lastResortKey.getId(),
                lastResortKey.getKeyPair().getPublicKey());

        final SignedPreKeyEntity signedPreKeyEntity = new SignedPreKeyEntity(
                signedPreKey.getId(),
                signedPreKey.getKeyPair().getPublicKey(),
                signedPreKey.getSignature());

        final long endTime = System.currentTimeMillis();
        final long elapsedSeconds = (endTime - startTime) / 1000;
        final long amendedTimestamp = timestamp + elapsedSeconds;

        final SignalBootstrap payload = new SignalBootstrap(
                entities,
                lastResortEntity,
                password,
                registrationId,
                signalingKey,
                signedPreKeyEntity,
                identityKey);

        final String payloadForSigning = JsonUtil.toJson(payload);

        this.signalInterface.register(payloadForSigning, amendedTimestamp)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleSubscriber<Void>() {
                    @Override
                    public void onSuccess(final Void unused) {
                        registrationSubscriber.onSuccess(unused);
                    }

                    @Override
                    public void onError(final Throwable throwable) {
                        registrationSubscriber.onError(throwable);
                    }
                });
    }
}
