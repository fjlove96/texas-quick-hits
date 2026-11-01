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
        cols = row.find_all(['td', 'th'])
        if len(cols) < 2:
            continue

        row_text = row.get_text()
        date_match = re.search(r'(\d{1,2})/(\d{1,2})/(\d{4})', row_text)
        if not date_match:
            continue
        
        m, d, y = int(date_match.group(1)), int(date_match.group(2)), int(date_match.group(3))

        # Extract numbers specifically from the table columns after the date column
        numbers = []
        for col in cols[1:]:
            found = re.findall(r'\b([1-9]|[1-4][0-9]|5[0-4])\b', col.get_text())
            for f in found:
                val = int(f)
                # Exclude month, day, or year numbers if they accidentally appear
                if val <= 54 and val != m and val != d and val != y and val != (y % 100):
                    numbers.append(val)

        if len(numbers) >= 6:
            nums = numbers[:6]
            new_entries.append(((y, m, d), f"Lotto Texas,{m},{d},{y}," + ",".join(str(n) for n in nums)))
            
    return new_entries

def main():
    existing = get_existing_dates()
    fetched = fetch_latest_draws()
    
    added_count = 0
    if fetched:
        fetched.sort(key=lambda item: item[0])
        # Re-write or update CSV cleanly
        # Let's clean up any incorrectly parsed rows from October 2026 first
        valid_lines = []
        if os.path.exists(CSV_FILE):
            with open(CSV_FILE, "r", encoding="utf-8") as f:
                for line in f:
                    parts = line.strip().split(",")
                    if len(parts) >= 10:
                        try:
                            mo, da, yr = int(parts[1]), int(parts[2]), int(parts[3])
                            n1, n2 = int(parts[4]), int(parts[5])
                            # If n1 == mo and n2 == da, it's a corrupted row from the old bug. Skip it so we re-fetch correctly!
                            if yr == 2026 and mo >= 9 and n1 == mo and n2 == da:
                                continue
                        except ValueError:
                            pass
                    valid_lines.append(line.strip())

        existing_valid = set()
        for line in valid_lines:
            parts = line.split(",")
            if len(parts) >= 10:
                try:
                    existing_valid.add((int(parts[3]), int(parts[1]), int(parts[2])))
                except ValueError:
                    pass

        with open(CSV_FILE, "w", encoding="utf-8") as f:
            for line in valid_lines:
                f.write(line + "\n")
            for date_tuple, csv_line in fetched:
                if date_tuple not in existing_valid:
                    f.write(csv_line + "\n")
                    print(f"Added new draw: {csv_line}")
                    added_count += 1
                    
    print(f"Sync finished. Added/Updated draws successfully.")

if __name__ == "__main__":
    main()
