#!/usr/bin/env python3

import requests
import json
from config import config

class admin_tool():
    header = {'Content-Type': 'application/json',
              'apiKey': config.api_key}

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
            response = requests.post('{}/api/v2/{}'.format(config.url, type),
                                     data=json.dumps(payload), headers=header)
        else:
            response = requests.put('{}/api/v2/{}/{}'.format(config.url, type, payload['id']),
                                    data=json.dumps(payload), headers=header)
        return response

    def task_manager():

        # open the file with our task geojson
        with open(config.geojson_file) as task_file:
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
            if len(tasks) >= 10000:
                upload_tasks(tasks)
                # reset the tasks list since the others were sent off
                tasks = []

        # if we end up here, we've finished the task creation. send off whats left.
        upload_tasks(tasks)

    def generate_tasks(geojson):
        pass

    def get_tasks_from_api(challenge_id):
        payload = {'limit': 10000}
        response = requests.get('{}/api/v2/challenge/{}/tasks'.format(config.url, challenge_id), headers=header, params=payload)
        return response

    def upload_tasks(tasks):
        print('Uploading {} tasks'.format(len(tasks)))
        response = requests.post('{}/api/v2/tasks'.format(config.url), data=json.dumps(tasks), headers=header)
        return response

    def update_tasks(tasks):
        print('Uploading {} tasks'.format(len(tasks)))
        response = requests.put('{}/api/v2/tasks'.format(config.url), data=json.dumps(tasks), headers=header)
        return response


if __name__ == '__main__':
    selection = None
    admin = admin_tool()
    while selection != 'q':
        selection = input(
'''Select One:
[1] Create Project
[2] Update Project
[3] Create Challenge
[4] Update Challenge
[5] Create Tasks
[q] Quit
==> ''')

        if (selection == '1'):
            admin_tool.manage('project', True)
        elif (selection == '2'):
            admin_tool.manage('project')
        elif (selection == '3'):
            admin_tool.manage('challenge', True)
        elif (selection == '4'):
            admin_tool.manage('challenge')
        elif (selection == '5'):
            admin_tool.task_manager()
        elif (selection == 'q'):
            break
        else:
            print("Please enter an option 1-6")

