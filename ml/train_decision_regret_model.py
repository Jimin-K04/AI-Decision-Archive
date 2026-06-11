import os
import json
import random
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report

SEED = 42
random.seed(SEED)
np.random.seed(SEED)
tf.keras.utils.set_random_seed(SEED)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CSV_PATH = os.path.join(BASE_DIR, "decision_training_data.csv")
TFLITE_PATH = os.path.join(BASE_DIR, "decision_regret_model.tflite")
INFO_PATH = os.path.join(BASE_DIR, "decision_model_info.json")

CATEGORY_MAP = {
    "진로": 0,
    "연애": 1,
    "인간관계": 2,
    "소비": 3,
    "공부": 4,
    "업무": 5,
    "음식": 6,
    "일상": 7,
}

LABELS = ["낮음", "보통", "높음"]

FEATURE_COLUMNS = [
    "category_id",
    "emotion_score",
    "reason_length",
    "choice_count",
    "temperature",
    "humidity",
    "discomfort_index",
    "hour",
]


def calculate_label(row):
    """
    regret_label:
    0 = 낮음
    1 = 보통
    2 = 높음

    초기 버전에서는 실제 회고 데이터가 부족하므로,
    앱에서 실제로 수집하는 입력값과 같은 구조의 샘플 데이터를 만들고
    규칙 기반 라벨을 붙여 머신러닝 모델을 학습한다.
    """

    score = 0

    emotion = row["emotion_score"]
    reason_length = row["reason_length"]
    choice_count = row["choice_count"]
    temperature = row["temperature"]
    humidity = row["humidity"]
    discomfort = row["discomfort_index"]
    hour = row["hour"]
    category_id = row["category_id"]

    # 감정 점수가 높을수록 감정에 밀린 결정일 가능성이 있다고 가정
    if emotion >= 6:
        score += 3
    elif emotion >= 4:
        score += 1

    # 선택 이유가 짧을수록 근거가 부족할 가능성이 있다고 가정
    if reason_length < 25:
        score += 3
    elif reason_length < 60:
        score += 1
    elif reason_length >= 100:
        score -= 2
    else:
        score -= 1

    # 선택지가 적으면 비교가 부족했을 가능성 반영
    if choice_count <= 1:
        score += 2
    elif choice_count >= 3:
        score -= 2

    # 날씨/컨디션 관련 요소
    if discomfort >= 80:
        score += 1

    if humidity >= 80:
        score += 1

    if temperature >= 30:
        score += 1

    # 밤늦은 시간 결정은 충동적일 가능성을 반영
    if hour >= 23 or hour <= 5:
        score += 1

    # 카테고리별 가정
    # 연애, 인간관계, 소비는 감정적 판단이나 후회 가능성이 상대적으로 높을 수 있다고 가정
    if category_id in [1, 2, 3]:
        score += 1

    # 공부, 업무, 진로는 비교적 계획 기반 판단이 많다고 가정
    if category_id in [0, 4, 5]:
        score -= 1

    # 라벨 기준
    # score가 낮으면 후회 가능성 낮음, 중간이면 보통, 높으면 높음
    if score <= 0:
        return 0
    elif score <= 3:
        return 1
    else:
        return 2


def generate_sample_data(n=1500):
    rows = []

    for _ in range(n):
        category_name = random.choice(list(CATEGORY_MAP.keys()))
        category_id = CATEGORY_MAP[category_name]

        emotion_score = random.randint(1, 7)

        # 선택 이유 길이: 10~180자
        reason_length = random.randint(10, 180)

        # 선택지 개수: 1~4개
        choice_count = random.randint(1, 4)

        # 날씨 데이터
        temperature = round(random.uniform(-5, 35), 1)
        humidity = random.randint(25, 95)

        # 간단한 불쾌지수 계산
        discomfort_index = round(
            0.81 * temperature + 0.01 * humidity * (0.99 * temperature - 14.3) + 46.3,
            1
        )

        hour = random.randint(0, 23)

        row = {
            "category_name": category_name,
            "category_id": category_id,
            "emotion_score": emotion_score,
            "reason_length": reason_length,
            "choice_count": choice_count,
            "temperature": temperature,
            "humidity": humidity,
            "discomfort_index": discomfort_index,
            "hour": hour,
        }

        row["regret_label"] = calculate_label(row)
        rows.append(row)

    return pd.DataFrame(rows)


def main():
    print("샘플 학습 데이터 생성 중...")

    df = generate_sample_data(1500)

    df.to_csv(CSV_PATH, index=False, encoding="utf-8-sig")
    print(f"CSV 저장 완료: {CSV_PATH}")
    print("라벨 분포:")
    print(df["regret_label"].value_counts().sort_index())

    X = df[FEATURE_COLUMNS].values.astype(np.float32)
    y = df["regret_label"].values.astype(np.int64)

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X).astype(np.float32)

    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled,
        y,
        test_size=0.2,
        random_state=SEED,
        stratify=y
    )

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(len(FEATURE_COLUMNS),)),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(8, activation="relu"),
        tf.keras.layers.Dense(3, activation="softmax")
    ])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.005),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    print("모델 학습 시작...")

    model.fit(
        X_train,
        y_train,
        epochs=100,
        batch_size=16,
        validation_split=0.2,
        verbose=1
    )

    print("테스트 평가...")
    y_pred_prob = model.predict(X_test)
    y_pred = np.argmax(y_pred_prob, axis=1)

    print(classification_report(y_test, y_pred, target_names=LABELS))

    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"테스트 정확도: {test_acc:.4f}")

    print("TensorFlow Lite 변환 중...")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    with open(TFLITE_PATH, "wb") as f:
        f.write(tflite_model)

    model_info = {
        "feature_columns": FEATURE_COLUMNS,
        "mean": scaler.mean_.tolist(),
        "scale": scaler.scale_.tolist(),
        "labels": LABELS,
        "category_map": CATEGORY_MAP
    }

    with open(INFO_PATH, "w", encoding="utf-8") as f:
        json.dump(model_info, f, ensure_ascii=False, indent=2)

    print(f"TFLite 모델 저장 완료: {TFLITE_PATH}")
    print(f"모델 정보 저장 완료: {INFO_PATH}")
    print("완료")


if __name__ == "__main__":
    main()