#!/usr/bin/env python3

import requests
import json

url = 'http://localhost:9000'
header = {'Content-Type': 'application/json',
          'apiKey': '2-gkXTYPGqJBIW3mP/fADklG9Tgy3JHYb0eeM6y2rR8Tgo1bNy/r1cuWBiRYTik0dtimjcbLxI'}

required = ['name', 'instruction', 'id']
fields = {'project': ['name', 'description'],
          'challenge': ['name', 'parent', 'identifier', 'difficulty',  'description',
                        'blurb', 'instruction']
         }

def manage(type, create=False):
    print('Enter each field or leave blank to skip')
    payload = {}

    # if it's being updated we need it's ID
    if not create:
        # Needs the ID field if being updated
        fields[type].append('id')

    # fill in all the fields for the api call
    for field in fields[type]:

        # if it's a required field, make sure it's filled in
        data = ''
        if field in required:
            while not data:
                print(field, 'is required')
                data = input(field + ': ')
        else:
            data = input(field + ': ')
        if data:
            payload[field] = data

    # Send off the payload to the appropriate endpoint
    response = ''
    if create:
        response = requests.post('{}/api/v2/{}'.format(url, type),
                                 data=json.dumps(payload), headers=header)
    else:
        response = requests.put('{}/api/v2/{}/{}'.format(url, type, payload['id']),
                                data=json.dumps(payload), headers=header)
    print(response.status_code, response.text)


def task_manager(update=True):

    # open the file with our task geojson
    with open('maproulette_tasks.geojson') as task_file:
        geojson = json.loads(task_file.read())
        tasks = []
        task_num = 1

        # we'll need the parent id for the api call
        parent = int(input('Enter parent id: '))

        # build a task for each item in the geojson array
        for feature in geojson['features']:
            task = {
                        'name': 'task-' + str(task_num),
                        'identifier': 'test-' + str(task_num),
                        'parent': parent,
                        'status': 0,
                        'geometries':
                        {
                            'type': 'FeatureCollection',
                            'features':
                            [{
                                'type': 'Feature',
                                'geometry': feature['geometry'],
                                'properties': {}
                            }]
                        }
                    }

            # change some values depending on what type of task we built
            if feature['properties']['type'] == 'Loop':
                task['instruction'] = 'This one way road loops back on itself. Edit it so that the road is properly accessible'
            else:
                task['instruction'] = 'This node is either unreachable or unleaveable. '
                task['instruction'] += 'Edit the surrounding roads so that the node can be accessed properly'

            # add the task to the list for later
            tasks.append(task)
            task_num += 1

            # if we have a significant number of tasks, push them up to the server so the size of
            # the request isn't too large
            if len(tasks) >= 20000:
                print('20,000 task capacity reached... uploading part... ', end='', flush=True)
                requests.post('{}/api/v2/tasks'.format(url), data=json.dumps(tasks), headers=header)
                print('Done!')

                # reset the tasks list since the others were sent off
                tasks = []

        # if we end up here, we've finished the task creation. send off whats left.
        print('Uploading {} tasks' % len(tasks))
        requests.post('{}/api/v2/tasks'.format(url), data=json.dumps(tasks), headers=header)


def get_tasks_from_api(challenge_id):
    response = requests.get('{}/api/v2/challenge/{}/children'.format(url, challenge_id), headers=header)
    print(response.status_code)


if __name__ == '__main__':
    selection = None
    while selection != 'q':
        selection = input(
'''Select One:
[1] Create Project
[2] Update Project
[3] Create Challenge
[4] Update Challenge
[5] Create Tasks
[6] Update Tasks
[q] Quit
==> ''')
        if (selection == '1'):
            manage('project', True)
        elif (selection == '2'):
            manage('project')
        elif (selection == '3'):
            manage('challenge', True)
        elif (selection == '4'):
            manage('challenge')
        elif (selection == '5'):
            task_manager()
        elif (selection == '6'):
            task_manager(update=True)
        elif (selection == 'q'):
            break
        else:
            print("Please enter an option 1-6")

