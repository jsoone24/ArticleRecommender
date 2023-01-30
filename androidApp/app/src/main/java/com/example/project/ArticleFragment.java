package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ArticleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArticleFragment extends Fragment implements IOnBackPressed, AdapterView.OnItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ArticleFragment(){}
    public ArticleFragment(ArrayList<ArticleVO> datas) {
        // Required empty public constructor
        this.datas = datas;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArticleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArticleFragment newInstance(String param1, String param2) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    ListView listView;
    ArticleAdapter adapter;
    long initTime;
    ArrayList<ArticleVO> datas;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        listView = view.findViewById(R.id.list_item);
        adapter = new ArticleAdapter(Objects.requireNonNull(getContext()), R.layout.article_layout, datas);
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);

        return view;
        //return inflater.inflate(R.layout.fragment_article, container, false);
    }

    @Override
    public boolean onBackPressed() {
        if (System.currentTimeMillis() - initTime > 3000) {
            showToast("종료하려면 한번 더 누르세요.");
            initTime = System.currentTimeMillis();
            return true;
        } else {
            getActivity().finish();
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ReadMode.class);
        intent.putExtra("pos", position);
        intent.putExtra("data", datas);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast t = Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), message, Toast.LENGTH_SHORT);
        t.show();
    }
}