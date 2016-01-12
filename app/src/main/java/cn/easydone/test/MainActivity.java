package cn.easydone.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity {

    private static final String TAG = "RxAppCompatActivity";

    private Subscription retrofitSubscription, zipSubscription;
    private UserAdapter userAdapter;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.setSmoothScrollbarEnabled(false);
        recyclerView.setLayoutManager(linearLayoutManager);
        realm = Realm.getDefaultInstance();
        List<User> userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        recyclerView.setAdapter(userAdapter);

        List<String> users = new ArrayList<>();
        users.add("liangzhitao");
        users.add("AlanCheen");
        users.add("yongjhih");
        users.add("zzz40500");
        users.add("greenrobot");
        users.add("nimengbo");

//        getUserOneByOne(users);
        getUserByZip(users);
    }

    private void getUserByZip(List<String> users) {
        List<Observable<GitHubUser>> observableList = new ArrayList<>();
        for (String user : users) {
            Observable<GitHubUser> observable = GitHubApiUtils.getInstance().getGitHubApi().user(user);
            observableList.add(observable);
        }

        zipSubscription = Observable.zip(observableList, args -> {
            List<GitHubUser> gitHubUsers = new ArrayList<>();
            for (Object arg : args) {
                gitHubUsers.add((GitHubUser) arg);
            }
            return gitHubUsers;
        }).compose(bindToLifecycle())
                .map(this::getUserList)
                .doOnNext(this::storeUserList)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(user1 -> realm.where(User.class).findAll())
                .subscribe(zipSubscriber);

    }

    private void storeUserList(List<User> users) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(users));
        realm.close();
    }

    Subscriber<List<User>> zipSubscriber = new Subscriber<List<User>>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onNext(List<User> users) {
            userAdapter.refreshView(users);
        }
    };

    private void getUserOneByOne(List<String> users) {
        retrofitSubscription = Observable.from(users)
                .compose(bindToLifecycle())
                .flatMap(s -> GitHubApiUtils.getInstance().getGitHubApi().user(s))
                .map(this::getUser)
                .doOnNext(this::storeUser)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(user1 -> realm.where(User.class).findAll())
                .subscribe(retrofitObserver);
    }

    private void storeUser(User user) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(user));
        realm.close();
    }

    @NonNull
    private List<User> getUserList(List<GitHubUser> gitHubUsers) {
        List<User> userList = new ArrayList<>();
        for (GitHubUser gitHubUser : gitHubUsers) {
            userList.add(getUser(gitHubUser));
        }
        return userList;
    }

    @NonNull
    private User getUser(GitHubUser gitHubUser) {
        User user = new User();
        user.setId(gitHubUser.id);
        user.setAvatarUrl(gitHubUser.avatarUrl);
        user.setName(gitHubUser.name);
        user.setBlog(gitHubUser.blog);
        user.setEmail(gitHubUser.email);
        user.setFollowers(gitHubUser.followers);
        user.setFollowing(gitHubUser.following);
        user.setPublicGists(gitHubUser.publicGists);
        user.setPublicRepos(gitHubUser.publicRepos);
        return user;
    }

    Observer<List<User>> retrofitObserver = new Observer<List<User>>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onNext(List<User> users) {
            userAdapter.refreshView(users);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (retrofitSubscription != null && !retrofitSubscription.isUnsubscribed()) {
            retrofitSubscription.unsubscribe();
        }
        if (zipSubscription != null && !zipSubscription.isUnsubscribed()) {
            zipSubscription.unsubscribe();
        }
        realm.close();
    }
}
