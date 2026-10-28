import os
import re
import requests
from bs4 import BeautifulSoup

CSV_FILE = "lottotexas.csv"

def get_existing_dates():
    existing_dates = set()
    if os.path.exists(CSV_FILE):
        with open(CSV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split(",")
                if len(parts) >= 10:
                    try:
                        m, d, y = int(parts[1]), int(parts[2]), int(parts[3])
                        existing_dates.add((y, m, d))
                    except ValueError:
                        continue
    return existing_dates

def fetch_latest_draws():
    url = "https://www.texaslottery.com/export/sites/lottery/Games/Lotto_Texas/Winning_Numbers/index.html"
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
    
    try:
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()
    except Exception as e:
        print(f"Error fetching Texas Lottery web page: {e}")
        return []

    soup = BeautifulSoup(response.text, "html.parser")
    rows = soup.find_all("tr")
    new_entries = []
    
    for row in rows:
        text = row.get_text()
        date_match = re.search(r'(\d{1,2})/(\d{1,2})/(\d{4})', text)
        if not date_match:
            continue
        
        m, d, y = int(date_match.group(1)), int(date_match.group(2)), int(date_match.group(3))
        numbers = re.findall(r'\b([1-9]|[1-4][0-9]|5[0-4])\b', text)
        if len(numbers) >= 6:
            nums = [int(n) for n in numbers[:6]]
            new_entries.append(((y, m, d), f"Lotto Texas,{m},{d},{y}," + ",".join(str(n) for n in nums)))
            
    return new_entries

def main():
    existing = get_existing_dates()
    fetched = fetch_latest_draws()
    
    added_count = 0
    if fetched and os.path.exists(CSV_FILE):
        fetched.sort(key=lambda item: item[0])
        with open(CSV_FILE, "a", encoding="utf-8") as f:
            for date_tuple, csv_line in fetched:
                if date_tuple not in existing:
                    f.write(csv_line + "\n")
                    print(f"Added new draw: {csv_line}")
                    added_count += 1
                    
    print(f"Sync finished. Added {added_count} new draw(s).")

if __name__ == "__main__":
    main()
