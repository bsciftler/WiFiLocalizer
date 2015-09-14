#!/usr/bin/python2.7
from __future__ import print_function, unicode_literals
import os
import sys
import json
import subprocess
from threading import Thread
from Queue import Queue, Empty
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler

path = '/tmp/pineap.log'
tailq = Queue(maxsize=30)


class RequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        data = RequestHandler.extract_lines(tailq)
        self.wfile.write(json.dumps({
            'results': data,
            'count': len(data),
        }) + '\n')

    @staticmethod
    def extract_lines(queue):
        data = []
        while True:
            try:
                data.append(queue.get_nowait())
            except Empty:
                return data


def tailf(fn):
    '''approach from http://stackoverflow.com/a/12523302/1832800'''
    p = subprocess.Popen(["tail", "-f", fn], stdout=subprocess.PIPE)

    while True:
        line = p.stdout.readline().rstrip()
        tailq.put(line)
        if not line:
            break


def server(path, port=80):
    '''https://docs.python.org/2/library/basehttpserver.html'''

    print('starting tail -f on {}'.format(path))
    Thread(target=tailf, args=(path,)).start()

    server_address = ('', port)
    print('starting server on 127.0.0.1:{}'.format(port))

    httpd = HTTPServer(server_address, RequestHandler)
    httpd.serve_forever()


if __name__ == '__main__':
    # accept one optional argument, either for help or for the dest of the log
    if len(sys.argv) > 1:
        if sys.argv[1] in ('-h', '--help'):
            print('./server.py [filename]')
            raise SystemExit(1)
        path = sys.argv[1]

    # die if file does not exist
    if not os.path.isfile(path):
        print('file {} does not exist'.format(path))
        raise SystemExit(1)

    server(path, port=8000)
