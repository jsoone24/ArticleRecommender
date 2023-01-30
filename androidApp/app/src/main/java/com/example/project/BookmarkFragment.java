package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BookmarkFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookmarkFragment extends Fragment implements AdapterView.OnItemClickListener { //북마크 된 기사들을 보여주는 북마크 프래그먼트 뷰이다.

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "BookmarkFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public BookmarkFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BookmarkFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookmarkFragment newInstance(String param1, String param2) {
        BookmarkFragment fragment = new BookmarkFragment();
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
    ArrayList<ArticleVO> datas;
    BookmarkAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        listView = view.findViewById(R.id.list_item);
        datas = new ArrayList<>();

        Realm realm = Realm.getDefaultInstance(); //NOSQL인 Realm에 북마크된 기사 목록을 불러온다.
        RealmResults<BookmarkVO> preDatas = realm.where(BookmarkVO.class).findAll();
        realm.beginTransaction();
        for (BookmarkVO d : preDatas) { //목록으로 보여주기 위해서 데이터 베이스 내부의 기사 정보들을 ArrayList에 저장한다.
            ArticleVO b = new ArticleVO();
            b.title = d.title;
            b.summary = d.summary;
            b.link = d.link;
            b.date = d.date;
            b.publisher = d.publisher;
            b.imageUri = d.imageUri;
            datas.add(b);
        }
        realm.commitTransaction();

        adapter = new BookmarkAdapter(Objects.requireNonNull(getContext()), R.layout.article_layout, datas); // 북마크 기사 목록을 어댑터로 만든다.
        listView.setAdapter(adapter); //리스트 뷰에 어댑터를 띄우고 선택이 가능 하도록 리스너를 설정한다.
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) { //북마크된 기사가 눌리면 이 동작이 실행된다.
        Intent intent = new Intent(getActivity(), ReadMode.class);
        intent.putExtra("pos", i); //어댑터 뷰 상에서 현재 눌린 기사의 현재 위치를 인텐트에 넣어주어 사용자가 읽기 모드에서 북마크 취소 시, 그 결과가 반영되게 한다.
        intent.putExtra("data", datas); //기사 정보를 또 한넘겨준다.
        startActivityForResult(intent, 30); //앱이 종료되고 동작을 수행하기 위해서, 이 인텐트에서 시작되었음을 구별하기 위해 requestcode를 설정해준다.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 30) { //requestcode를 설정해 의도한 코드가 맞을 경우 동작을 수행한다.
            try {
                int i = data.getIntExtra("posreturn", -1); //삭제가 아닐 경우 i = -1이다. 이는 널포인터 예외를 발생시켜 그냥 빠져나온다. 삭제일 경우, 예외가 발생하지 않아 삭제가 진행된다.
                datas.remove(i);
                adapter.notifyDataSetChanged();
            } catch (NullPointerException e) {
            }
        }
    }
}