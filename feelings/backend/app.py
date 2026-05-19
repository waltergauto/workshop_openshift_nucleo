from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

positive_words = {
    "happy", "good", "great", "excellent", "amazing", "wonderful",
    "fantastic", "beautiful", "love", "awesome", "brilliant", "superb",
    "delighted", "joy", "perfect", "nice", "kind", "best", "positive",
    "hope", "success", "win", "celebrate", "grateful", "thankful",
    "incredible", "magnificent", "splendid", "marvelous", "terrific",
    "fabulous", "glorious", "heavenly", "divine", "lovely", "charming",
    "enjoy", "exciting", "thrilling", "pleasure", "satisfied", "happy",
    "cheerful", "optimistic", "peaceful", "wonderful", "fun", "hilarious",
    "outstanding", "remarkable", "impressive", "genius", "smart",
}

negative_words = {
    "sad", "bad", "terrible", "awful", "horrible", "hate", "angry",
    "ugly", "worst", "disgusting", "depressing", "horrific", "nasty",
    "fear", "pain", "cry", "hurt", "fail", "loss", "tragic", "dreadful",
    "miserable", "horrendous", "hideous", "grim", "somber", "gloomy",
    "dreary", "dismal", "bleak", "hopeless", "despair", "sorrow",
    "grief", "mourn", "regret", "shame", "guilt", "jealous", "selfish",
    "rude", "cruel", "violent", "dangerous", "poisonous", "toxic",
    "annoying", "frustrating", "infuriating", "enraging", "awful",
    "pathetic", "useless", "worthless", "stupid", "dumb", "ugly",
}


def analyze_sentiment(text: str) -> str:
    words = text.lower().split()
    score = 0
    for word in words:
        if word in positive_words:
            score += 1
        if word in negative_words:
            score -= 1
    return "positive" if score >= 0 else "negative"


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json(force=True)
    text = data.get("text", "").strip()
    if not text:
        return jsonify({"error": "No text provided"}), 400

    sentiment = analyze_sentiment(text)
    return jsonify({
        "sentiment": sentiment,
        "text": text,
        "service": "feelings-backend"
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
