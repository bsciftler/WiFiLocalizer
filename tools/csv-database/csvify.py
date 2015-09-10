#!/usr/bin/env python3

import csv
from bs4 import BeautifulSoup


def main():
    FN = './stripped.html'
    HEADER = ['datetime', 'mapx', 'mapy', 'rss', 'ap_name', 'mac', 'map']

    soup = BeautifulSoup(open(FN))
    with open('data.csv', 'w') as f:
        writer = csv.writer(f)
        writer.writerow(HEADER)
        for tr in soup.html.table.children:
            if tr != '\n':
                row = tr.get_text().split('\n')[2:-1]
                writer.writerow(row)


if __name__ == '__main__':
    main()
