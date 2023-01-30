package com.example.project;

public interface IOnBackPressed { //프래그먼트의 뒤로가기를 제어하기 위한 인터페이스이다.
    /**
     * If you return true the back press will not be taken into account, otherwise the activity will act naturally
     *
     * @return true if your processing has priority if not false
     */
    boolean onBackPressed();
}
