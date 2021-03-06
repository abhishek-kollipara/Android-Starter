package com.kinvey.bookshelf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyListCallback;
import com.kinvey.android.callback.KinveyPurgeCallback;
import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.android.sync.KinveyPullCallback;
import com.kinvey.android.sync.KinveyPullResponse;
import com.kinvey.android.sync.KinveyPushCallback;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;
import com.kinvey.java.cache.KinveyCachedClientCallback;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.dto.User;
import com.kinvey.java.store.StoreType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShelfActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private final String TAG = ShelfActivity.class.getSimpleName();

    private Client client;
    private BooksAdapter adapter;
    private DataStore<Book> bookStore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client = ((App) getApplication()).getSharedClient();
        bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book.class, StoreType.CACHE, client);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            checkLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        dismissProgress();
        super.onStop();
    }

    private void sync() {
        showProgress(getResources().getString(R.string.progress_sync));
        bookStore.sync(new KinveySyncCallback<Book>() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<Book> kinveyPullResponse) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_sync_completed, Toast.LENGTH_LONG).show();
                getData();
            }

            @Override
            public void onPullStarted() {

            }

            @Override
            public void onPushStarted() {

            }

            @Override
            public void onPullSuccess(KinveyPullResponse<Book> kinveyPullResponse) {

            }

            @Override
            public void onPushSuccess(KinveyPushResponse kinveyPushResponse) {

            }

            @Override
            public void onFailure(Throwable t) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_sync_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getData() {
        bookStore.find(new KinveyListCallback<Book>() {
            @Override
            public void onSuccess(List<Book> books) {
                updateBookAdapter(books);
                Log.d(TAG, "ListCallback: success");
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, "ListCallback: failure");
            }
        }, new KinveyCachedClientCallback<List<Book>>() {
            @Override
            public void onSuccess(final List<Book> books) {
                Log.d(TAG, "CachedClientCallback: success");
                updateBookAdapter(books);
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.d(TAG, "CachedClientCallback: failure");
            }
        });
    }

    private void updateBookAdapter(List<Book> books) {
        if (books == null) {
            books = new ArrayList<Book>();
        }
        ListView list = (ListView) findViewById(R.id.shelf);
        list.setOnItemClickListener(ShelfActivity.this);
        adapter = new BooksAdapter(books, ShelfActivity.this);
        list.setAdapter(adapter);
    }

    private void checkLogin() throws IOException {
        if (!client.isUserLoggedIn()) {
            showProgress(getResources().getString(R.string.progress_login));
            UserStore.login(Constants.USER_NAME, Constants.USER_PASSWORD, client, new KinveyClientCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    //successfully logged in
                    dismissProgress();
                    Toast.makeText(ShelfActivity.this, R.string.toast_sign_in_completed, Toast.LENGTH_LONG).show();
                    sync();
                }

                @Override
                public void onFailure(Throwable error) {
                    UserStore.signUp(Constants.USER_NAME, Constants.USER_PASSWORD, client, new KinveyClientCallback<User>() {
                        @Override
                        public void onSuccess(User o) {
                            getData();
                            dismissProgress();
                            Toast.makeText(ShelfActivity.this, R.string.toast_sign_up_completed, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            dismissProgress();
                            Toast.makeText(ShelfActivity.this, R.string.toast_can_not_login, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            getData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shelf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_new:
                Intent i = new Intent(this, BookActivity.class);
                startActivity(i);
                return true;
            case R.id.action_sync:
                sync();
                break;
            case R.id.action_pull:
                pull();
                break;
            case R.id.action_push:
                push();
                break;
            case R.id.action_purge:
                purge();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pull() {
        showProgress(getResources().getString(R.string.progress_pull));
        bookStore.pull(null, new KinveyPullCallback<Book>() {
            @Override
            public void onSuccess(KinveyPullResponse kinveyPullResponse) {
                dismissProgress();
                getData();
                Toast.makeText(ShelfActivity.this, R.string.toast_pull_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_pull_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void push() {
        showProgress(getResources().getString(R.string.progress_push));
        bookStore.push(new KinveyPushCallback() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_push_completed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_push_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(long current, long all) {

            }
        });
    }

    private void purge() {
        showProgress(getResources().getString(R.string.progress_purge));
        bookStore.purge(new KinveyPurgeCallback() {
            @Override
            public void onSuccess(Void result) {
                dismissProgress();
                getData();
                Toast.makeText(ShelfActivity.this, R.string.toast_purge_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_purge_failed, Toast.LENGTH_LONG).show();
            }

        });
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Book book = adapter.getItem(position);
        Intent i = new Intent(this, BookActivity.class);
        i.putExtra(Constants.EXTRA_ID, book.get(Constants.ID).toString());
        startActivity(i);
    }
}
