import pandas as pd
import os.path
from datetime import date, timedelta, datetime
import datetime
import errno


def main():
    categorySid1 = {'100': ['264', '265', '268', '266', '267', '269'],
                    '101': ['259', '258', '261', '771', '260', '262', '310', '263'],
                    '102': ['249', '250', '251', '254', '252', '59', '255', '256', '276', '257'],
                    '103': ['241', '239', '240', '237', '238', '376', '242', '243', '244', '248', '245'],
                    '104': ['231', '232', '233', '234', '322'],
                    '105': ['731', '226', '227', '230', '732', '283', '229', '228']}
    start = datetime.datetime.strptime('2020-09-06', '%Y-%m-%d')  # datetime.date(start_day) #탐색 시작 날짜, 끝 날짜
    end = datetime.datetime.strptime('2020-09-07', '%Y-%m-%d')  # datetime.date(datetime.now())

    while start <= end:
        articleSUM = pd.DataFrame()
        articleDB = pd.DataFrame()
        for sid1 in categorySid1:
            for sid2 in categorySid1[sid1]:
                articleInfo = pd.DataFrame()
                articleInfo_dir = "./articleInfo/" + start.strftime('%Y-%m-%d') + "/" + sid1 + "/"  # 파일 경로
                articleInfo_filename = "sid2_" + sid2 + ".csv"  # 파일 이름
                try:
                    articleInfo = pd.read_csv(articleInfo_dir + articleInfo_filename)  # 크롤링한 데이터 호출
                except:
                    break
                articleInfo["aid"] = articleInfo["aid"].apply(lambda x: '{0:0>10}'.format(x))
                articleInfo["oid"] = articleInfo["oid"].apply(lambda x: '{0:0>3}'.format(x))  # 자릿수에 맞춰 수정
                articleInfo.drop(articleInfo.columns[[0, 6, 7]], axis=1, inplace=True)  # Untitle, 링크, 요약 column 제거
                for i in range(5):
                    articleInfo["키워드 " + str(i)] = articleInfo['sid1'].astype(str) + "_" + articleInfo['sid2'].astype(
                        str) + "_" + articleInfo["키워드 " + str(i)].astype(str)
                    # sid1_sid2_키워드 형식으로 키워드 수정
                if len(articleSUM) == 0:  # 소분류별로 생성되었던 파일을 날짜별로 만들기 위해 통합
                    articleSUM = articleInfo
                else:
                    articleSUM = articleSUM.append(articleInfo, sort=True)

        for i in range(5):
            articleRE = pd.DataFrame()
            articleRE = articleSUM.reset_index().groupby(['aid', 'oid', 'sid1', 'sid2', '본문', '키워드 ' + str(i)])[
                '키워드 ' + str(i) + ' 가중치'].aggregate('first').unstack().fillna(0)
            # column의 인덱스에 키워드 값이 들어가고 value값에 가중치가 들어가도록 수정
            if len(articleDB) == 0:  # 전체 키워드(여기서는 0,1,2,3,4)를 하나의 데이터 프레임으로 연결
                articleDB = articleRE
            else:
                articleDB = articleDB.add(articleRE, fill_value=0)
        for sid1 in categorySid1:
            for sid2 in categorySid1[sid1]:
                try:
                    articleDB.drop(str(sid1) + "_" + str(sid2) + "_nan", axis=1, inplace=True)  # 전체 nan 값 삭제
                except:
                    pass
        try:  # 파일 경로 생성, 경로가 존재 하지 않을 경우 파일 경로 생성
            if not (os.path.isdir("./articleDB")):
                os.makedirs(os.path.join("./articleDB"))
        except OSError as e:
            if e.errno != errno.EEXIST:
                print("Dir error")
            raise
        articleDB.to_csv("./articleDB/" + start.strftime('%Y-%m-%d') + "_DB.csv")  # csv파일로 저장
        start += timedelta(1)  # 날짜가 끝나면 날짜 더해서 한번 더


if __name__ == "__main__":
    main()