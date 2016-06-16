#!/usr/bin/python3

import requests
import json

def manage(type, create=False):
    print('Enter each field or leave blank to skip')
    payload = {}
    fields = []
    # Set fields for type of request
    if type == 'project':
        fields = ['name', 'description']
    elif type == 'challenge':
        fields = ['name', 'parent', 'identifier', 'difficulty',  'description',
                  'blurb', 'instruction']
    if not create:
        # Needs the ID field if being updated
        fields.append('id')
    for field in fields:
        d = ''
        if field == 'name' or (not create and field == 'id'):
            while not d:
                print(field, 'is required')
                d = input(field + ': ')
        else:
            d = input(field + ': ')
        if d:
            payload[field] = d
    header = {'Content-Type': 'text/json'}
    response = ''
    if create:
        response = requests.post('http://localhost:9000/api/v2/{}'.format(type),
                                 data=json.dumps(payload), headers=header)
        print(response.status_code)
        print(response.text)
    else:
        response = requests.put('http://localhost:9000/api/v2/{}'.format(type),
                                data=json.dumps(data), headers=header)
        print(response.status_code)
        print(response.text)

if __name__ == '__main__':
    while True:
        r = input(
'''Select One:
[1] Create Project
[2] Update Project
[3] Create Challenge
[4] Update Challenge
==> ''')
        if (r == '1'):
            manage('project', True)
            break
        elif (r == '2'):
            manage('project')
            break
        elif (r == '3'):
            manage('challenge', True)
            break
        elif (r == '4'):
            manage('challenge')
            break
        else:
            print("Please enter an option 1-4")

