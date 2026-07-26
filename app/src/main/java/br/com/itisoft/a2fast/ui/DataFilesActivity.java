package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.R;
import br.com.itisoft.a2fast.data.DataSession;
import br.com.itisoft.a2fast.model.DatafileEntry;

/**
 * Lists registered {@code .2fa} files and lets the user open, add, or remove them.
 */
public class DataFilesActivity extends AppCompatActivity {

    public static final String EXTRA_MANAGE_ONLY = "manage_only";

    private DatafileAdapter adapter;
    private View emptyContainer;
    private View coordinator;
    private boolean manageOnly;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datafiles);

        manageOnly = getIntent().getBooleanExtra(EXTRA_MANAGE_ONLY, false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.datafiles_title);
            if (manageOnly) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        emptyContainer = findViewById(R.id.emptyDatafiles);
        coordinator = findViewById(R.id.coordinatorDatafiles);
        RecyclerView list = findViewById(R.id.recyclerDatafiles);
        FloatingActionButton fab = findViewById(R.id.fabAddDatafile);
        View emptyAdd = findViewById(R.id.btnEmptyAddFile);

        adapter = new DatafileAdapter(new DatafileAdapter.Listener() {
            @Override
            public void onOpen(DatafileEntry entry) {
                openFile(entry);
            }

            @Override
            public void onRemove(DatafileEntry entry) {
                confirmRemove(entry);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        View.OnClickListener addClick = v -> showAddOptions();
        fab.setOnClickListener(addClick);
        emptyAdd.setOnClickListener(addClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        if (!manageOnly && !App.get().preferences().hasConfiguredDatafile()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_datafiles, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_create) {
            startActivity(new Intent(this, CreateDataFileActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_add_open) {
            startActivity(new Intent(this, OpenDataFileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshList() {
        List<DatafileEntry> files = App.get().preferences().getDatafiles();
        String activeId = App.get().preferences().getActiveDatafileId();
        adapter.submit(files, activeId);
        emptyContainer.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddOptions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_datafile_title)
                .setItems(new CharSequence[]{
                        getString(R.string.create_datafile),
                        getString(R.string.open_datafile)
                }, (d, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, CreateDataFileActivity.class));
                    } else {
                        startActivity(new Intent(this, OpenDataFileActivity.class));
                    }
                })
                .show();
    }

    private void openFile(DatafileEntry entry) {
        App.get().preferences().setActiveDatafileId(entry.id);
        if (DataSession.get().isLoaded()
                && entry.id.equals(DataSession.get().getActiveFileId())) {
            startActivity(new Intent(this, AccountListActivity.class));
            return;
        }
        DataSession.get().lock();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_FILE_ID, entry.id);
        startActivity(intent);
    }

    private void confirmRemove(DatafileEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_datafile_title)
                .setMessage(getString(R.string.remove_datafile_message, entry.displayName))
                .setPositiveButton(R.string.remove_from_app, (d, w) -> {
                    boolean wasActive = entry.id.equals(DataSession.get().getActiveFileId());
                    App.get().passwordVault().removePasswordForFile(entry.id, entry.passwordHash);
                    App.get().preferences().removeDatafile(entry.id);
                    if (wasActive) {
                        DataSession.get().lock();
                    }
                    refreshList();
                    Snackbar.make(coordinator, R.string.datafile_removed, Snackbar.LENGTH_SHORT).show();
                    if (!App.get().preferences().hasConfiguredDatafile() && !manageOnly) {
                        Intent intent = new Intent(this, WelcomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static final class DatafileAdapter extends RecyclerView.Adapter<DatafileAdapter.Holder> {

        interface Listener {
            void onOpen(DatafileEntry entry);

            void onRemove(DatafileEntry entry);
        }

        private final Listener listener;
        private final List<DatafileEntry> items = new ArrayList<>();
        private String activeId;

        DatafileAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<DatafileEntry> entries, String activeId) {
            items.clear();
            items.addAll(entries);
            this.activeId = activeId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_datafile, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            DatafileEntry entry = items.get(position);
            holder.title.setText(entry.displayName);
            holder.subtitle.setText(friendlyLocation(entry.uri));
            boolean active = entry.id != null && entry.id.equals(activeId);
            holder.badge.setVisibility(active ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onOpen(entry));
            holder.itemView.setOnLongClickListener(v -> {
                listener.onRemove(entry);
                return true;
            });
            holder.more.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, R.string.remove_from_app);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        listener.onRemove(entry);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        private static String friendlyLocation(String uriString) {
            if (uriString == null || uriString.isEmpty()) {
                return "";
            }
            try {
                Uri uri = Uri.parse(uriString);
                String last = uri.getLastPathSegment();
                if (last != null && !last.isEmpty()) {
                    // SAF often encodes the display path after a colon.
                    int colon = last.lastIndexOf(':');
                    if (colon >= 0 && colon < last.length() - 1) {
                        last = last.substring(colon + 1);
                    }
                    return last.replace("%2F", "/").replace("%20", " ");
                }
            } catch (Exception ignored) {
                // fall through
            }
            return uriString;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView subtitle;
            final TextView badge;
            final ImageButton more;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textFileName);
                subtitle = itemView.findViewById(R.id.textFileUri);
                badge = itemView.findViewById(R.id.textActiveBadge);
                more = itemView.findViewById(R.id.btnMoreFile);
            }
        }
    }
}
