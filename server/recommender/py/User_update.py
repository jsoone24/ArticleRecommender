import pandas as pd
import os.path
import errno
import sqlite3
import re
import datefinder
import datetime
import sys
from multiprocessing import Pool


def main(ID):
    con = sqlite3.connect('./recommender/userprofile/'+ID+'/recorddb')
    cur = con.cursor()
    cur.execute("SELECT * FROM tb_record")
    rows = cur.fetchall()
    cols = [column[0] for column in cur.description]
    read_data = pd.DataFrame.from_records(data=rows, columns=cols)
    con.close()
    #읽은 기사 목록 데이터 베이스 불러오기

    date_DB = pd.read_csv("./recommender/py/articleDB/2020-09-05_DB.csv")
    date_DB["aid"] = date_DB["aid"].apply(lambda x: '{0:0>10}'.format(x)) #비교할 기사 목록 데이터 베이스 불러오기
    Save = pd.DataFrame()
    for j in range(len(read_data.index)):
        link = read_data['link'][j] #읽은 기사 목록에서 링크 추출
        aid_find = re.findall('aid=[0-9]+', link)[0][4:] #읽은 기사 목록에서 aid 추출

        articledate = read_data['articledate'][j]
        date = list(datefinder.find_dates(articledate))[0] #읽은 기사 목록에서 날짜 추출
        article_date = date.strftime('%Y-%m-%d')
        if(article_date != '2020-09-05'): #현재 20-09-05만 비교하므로 해당하지 않으면 비교하지 않음
            continue

        compare_DB = date_DB[date_DB['aid'].isin([aid_find])]
        compare_DB = compare_DB.drop(["aid", "oid", "sid1", "sid2", "본문"], axis=1)
        compare_DB = compare_DB[0:1] #비교할 데이터만 남기고 drop

        try:
            User = pd.read_csv('./recommender/userprofile/'+ID+'/UserData.csv') #저장되어있는 유저데이터 불러오기
        except:
            User = pd.DataFrame()
        User = pd.concat([User, compare_DB], axis=0)
        User.reset_index(drop=True, inplace=True)
        try:
            User.drop([User.index[1]], inplace=True)
            User.fillna(1 / User.shape[1], inplace=True)
        except:
            User.iloc[:, :] = 1 / User.shape[1]
        User.sort_index(axis=1, inplace=True) #없는 키워드 최소값으로 추가(전체 값의 합이 1이 되도록)

        compare_DB = pd.concat([compare_DB, User], axis=0)
        compare_DB.reset_index(drop=True, inplace=True)
        compare_DB.drop([compare_DB.index[1]], inplace=True)
        compare_DB.fillna(0, inplace=True)
        compare_DB.sort_index(axis=1, inplace=True)

        c_compare_DB = compare_DB.reset_index(drop=True)*100 + 1 #가중치의 의미를 키우기 위해 100곱하기
        User = User.mul(c_compare_DB) #기사 키워드 가중치와 유저 키워드 가중치 곱하여 다음 가중치 계산
        User_sum = User.sum(axis=1)[0]
        User = User / User_sum #총합이 1이 되도록 계산

        Save_User = pd.DataFrame()
        for i in User.columns:
            if (all(User[i] != User.min(axis=1)[0])):
                Save_User.insert(0, i, User[i])
        Save_User.reset_index(drop=True, inplace=True)

        Save_User = Save_User.dropna(axis=1)
        Save = pd.concat([Save, Save_User], axis=1)

    Save.to_csv('./recommender/userprofile/'+ID+'/UserData.csv') #유저데이터 갱신


if __name__ == "__main__":
    main(sys.argv[1])