package com.thachpham.mentionexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.thachpham.mention.MentionEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final User[] USERS = new User[]{
            new User("Baron"),
            new User("Thach"),
            new User("Sanji"),
            new User("Mc Anrahoka"),
            new User("Basto Minen"),
            new User("Alex Kuro"),
            new User("Hansen Nar"),
            new User("Hazar Fona"),
            new User("Phoenix"),
            new User("Hon rada"),
            new User("Bookiesa Tanra"),
    };

    private List<User> users = new ArrayList<>();

    private MentionEditText mentionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.rvListMention);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final Adapter adapter = new Adapter();
        recyclerView.setAdapter(adapter);

        mentionEditText = findViewById(R.id.edtMention);
        mentionEditText.setActionListener(new MentionEditText.ActionListener() {
            @Override
            public void onNeedOpenListMention() {
                users.addAll(Arrays.asList(USERS));
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNeedCloseListMention() {
                recyclerView.setVisibility(View.GONE);
            }

            @Override
            public boolean onSearchListMention(String keyword) {
                users.clear();
                if (keyword.equals("")) {
                    users.addAll(Arrays.asList(USERS));
                } else {
                    for (User user : USERS) {
                        if (keyword.length() >= user.getName().length()) {
                            continue;
                        }
                        String compareString = user.getName().substring(0, keyword.length()).toLowerCase();
                        if (compareString.equals(keyword.toLowerCase()))
                            users.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
                return !users.isEmpty();
            }
        });

        findViewById(R.id.btAddDenotation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mentionEditText.addDenotation();
            }
        });
    }

    class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserVH(LayoutInflater.from(MainActivity.this).inflate(R.layout.row_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
            ((UserVH) holder).tvName.setText(users.get(position).getName());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mentionEditText.addMention(users.get(holder.getAdapterPosition()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    class UserVH extends RecyclerView.ViewHolder {

        TextView tvName;

        public UserVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}
