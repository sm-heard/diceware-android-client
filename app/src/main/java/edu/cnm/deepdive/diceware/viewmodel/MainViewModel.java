package edu.cnm.deepdive.diceware.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import edu.cnm.deepdive.diceware.R;
import edu.cnm.deepdive.diceware.model.Passphrase;
import edu.cnm.deepdive.diceware.service.DicewareService;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

  private final MutableLiveData<List<Passphrase>> passphrases =
      new MutableLiveData<>();
  private final MutableLiveData<GoogleSignInAccount> account =
      new MutableLiveData<>();
  private final MutableLiveData<Throwable> throwable = new MutableLiveData<>();
  private final DicewareService dicewareService = DicewareService.getInstance();

  public MainViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<Passphrase>> getPassphrases() {
    return passphrases;
  }

  public LiveData<Throwable> getThrowable() {
    return throwable;
  }

  public void setAccount(GoogleSignInAccount account) {
    this.account.setValue(account);
    refreshPassphrases();
  }

  public void deletePassphrase(Passphrase passphrase) {
    GoogleSignInAccount account = this.account.getValue();
    if (passphrase != null && passphrase.getId() > 0 && account !=null) {
      String token = getApplication().getString(R.string.oauth_header, account.getIdToken());
      dicewareService.delete(token, passphrase.getId())
          .subscribeOn(Schedulers.io())
          .subscribe(
              () -> refreshPassphrases(account),
              (throwable) -> this.throwable.postValue(throwable)
          );
    }
  }

  public void refreshPassphrases() {
    GoogleSignInAccount account = this.account.getValue();
    if (account != null) {
      refreshPassphrases(account);
    } else {
      passphrases.setValue(Collections.EMPTY_LIST);
    }
  }

  public void addPassphrase(Passphrase passphrase) {
    GoogleSignInAccount account = this.account.getValue();
    if (account != null) {
      String token = getApplication().getString(R.string.oauth_header, account.getIdToken());
      dicewareService.post(token, passphrase)
          .subscribeOn(Schedulers.io())
          .subscribe(
              (p) -> refreshPassphrases(account),
              (throwable) -> this.throwable.postValue(throwable)
          );
    }
  }

  public void updatePassphrase(Passphrase passphrase) {
    GoogleSignInAccount account = this.account.getValue();
    if (account != null) {
      String token = getApplication().getString(R.string.oauth_header, account.getIdToken());
      dicewareService.put(token, passphrase.getId(), passphrase)
          .subscribeOn(Schedulers.io())
          .subscribe(
              (p) -> refreshPassphrases(account),
              (throwable) -> this.throwable.postValue(throwable)
          );
    }
  }

  private void refreshPassphrases(GoogleSignInAccount account) {
    String token = getApplication().getString(R.string.oauth_header, account.getIdToken());
    Log.d("Oauth2.0 token", token); // FIXME Remove before shipping.
    dicewareService.getAll(token)
        .subscribeOn(Schedulers.io())
        .subscribe(
            (passphrases) -> this.passphrases.postValue(passphrases),
            (throwable) -> this.throwable.postValue(throwable)
        );
  }

}
