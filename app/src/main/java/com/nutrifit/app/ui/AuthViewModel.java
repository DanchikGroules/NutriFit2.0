package com.nutrifit.app.ui;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.nutrifit.app.data.LocalRepository;

public class AuthViewModel extends AndroidViewModel {
  // 0 idle, 1 working, 2 success, 3 credentials rejected, 4 storage error.
  public final MutableLiveData<Integer> state = new MutableLiveData<>(0);
  public String recoveryCode;

  public AuthViewModel(Application app) {
    super(app);
  }

  public void demo(String name,String language) {
    if(Integer.valueOf(1).equals(state.getValue()))return;
    state.setValue(1);
    LocalRepository.get(getApplication()).io.execute(()->{
      try {LocalRepository.startDemo(getApplication(),name,language);state.postValue(2);}
      catch(Exception e){state.postValue(4);}
    });
  }

  public void submit(boolean register, String name, String email, char[] password) {
    if (Integer.valueOf(1).equals(state.getValue())) {
      java.util.Arrays.fill(password, '\0');
      return;
    }
    state.setValue(1);
    LocalRepository repo = LocalRepository.get(getApplication());
    repo.io.execute(
        () -> {
          try {
            boolean success;
            if (register) {
              recoveryCode = repo.account.register(name, email, password);
              success = true;
            } else success = repo.account.login(email, password);
            state.postValue(success ? 2 : 3);
          } catch (Exception e) {
            state.postValue("throttled".equals(e.getMessage()) ? 5 : 4);
          } finally {
            java.util.Arrays.fill(password, '\0');
          }
        });
  }
}
