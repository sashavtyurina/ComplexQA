#!/usr/bin/env python
# encoding: utf-8
"""
Bing Search API
---------------

Usage:

```python

import bing

api = bing.Api('your-key')

web_results = api.query('hello world')
news_results = api.query('hello world', srctype='News')

```
------------------------------------------------------------
"""
from __future__ import unicode_literals

import base64
import urllib.parse
import logging

try:
    import requests
except ImportError:
    logging.warning(
        'Cannot import `requests` module. '
        'Make sure you write your own fetch function'
    )


__version__ = '0.1.1'


def enquote(value):
    return "'{}'".format(value)


def double(value):
    return float(value)


class BingSourceTypeParameters(object):
    def __init__(self):
        self.bing_srctype_params = {
            'Composite': {
                'Query':                enquote,  # xbox
                'Sources':              enquote,  # web+image+video+news+spell
                'Adult':                enquote,  # Moderate
                'ImageFilters':         enquote,  # Size:Small+Aspect:Square
                'Latitude':             double,   # 47.603450
                'Longitude':            double,   # -122.329696
                'Market':               enquote,  # en-US
                'NewsCategory':         enquote,  # rt_Business
                'NewsLocationOverride': enquote,  # US.WA
                'NewsSortBy':           enquote,  # Date
                'Options':              enquote,  # EnableHighlighting
                'VideoFilters':         enquote,  # Duration:Short+Resolution:High
                'VideoSortBy':          enquote,  # Date
                'WebFileType':          enquote,  # XLS
                'WebSearchOptions':     enquote,  # DisableQueryAlterations
            },

            'Web': {
                'Query':                enquote,  # xbox
                'Adult':                enquote,  # Moderate
                'Latitude':             double,   # 47.603450
                'Longitude':            double,   # -122.329696
                'Market':               enquote,  # en-US
                'Options':              enquote,  # EnableHighlighting
                'WebFileType':          enquote,  # XLS
                'WebSearchOptions':     enquote,  # DisableQueryAlterations
            },

            'Image': {
                'Query':                enquote,  # xbox
                'Adult':                enquote,  # Moderate
                'ImageFilters':         enquote,  # Size:Small+Aspect:Square
                'Latitude':             double,   # 47.603450
                'Longitude':            double,   # -122.329696
                'Market':               enquote,  # en-US
                'Options':              enquote,  # EnableHighlighting
            },

            'Video': {
                'Query':                enquote,  # xbox
                'Adult':                enquote,  # Moderate
                'Latitude':             double,   # 47.603450
                'Longitude':            double,   # -122.329696
                'Market':               enquote,  # en-US
                'Options':              enquote,  # EnableHighlighting
                'VideoFilters':         enquote,  # Duration:Short+Resolution:High
                'VideoSortBy':          enquote,  # Date
            },

            'News': {
                'Query':                enquote,    # xbox
                'Adult':                enquote,    # Moderate
                'Latitude':             double,     # 47.603450
                'Longitude':            double,     # -122.329696
                'Market':               enquote,    # en-US
                'NewsCategory':         enquote,    # rt_Business
                'NewsLocationOverride': enquote,    # US.WA
                'NewsSortBy':           enquote,    # Date
                'Options':              enquote,    # EnableHighlighting
            },

            'RelatedSearch': {
                'Query':                enquote,    # xbox
                'Adult':                enquote,    # Moderate
                'Latitude':             double,     # 47.603450
                'Longitude':            double,     # -122.329696
                'Market':               enquote,    # en-US
                'Options':              enquote,    # EnableHighlighting
            },

            'SpellingSuggestion': {
                'Query':                enquote,    # xblox
                'Adult':                enquote,    # Moderate
                'Latitude':             double,     # 47.603450
                'Longitude':            double,     # -122.329696
                'Market':               enquote,    # en-US
                'Options':              enquote,    # EnableHighlighting
            }
        }


class BingError(Exception):
    """ General exception for Bing errors. """
    pass


class Api(object):
    """ Interacting with Bing APIs """
    BING_SRCTYPE_PARAMS = BingSourceTypeParameters().bing_srctype_params
    BING_URL = ('https://api.datamarket.azure.com/Bing/Search/v1/{source_type}')

    def __init__(self, key):
        self.key = key

    @staticmethod
    def fetch(url, account_key, parameters):
        """ Fetch from remote server
         Basic authentication: username - empty, password - account_key """

        req = requests.get(url, auth=('', account_key), params=parameters)
        return req

    def check(self, srctype, sources, extra):
        """ Check if srctype, sources and the co. are correct """

        if srctype not in self.BING_SRCTYPE_PARAMS:
            raise BingError(
                'Invalid `srctype`. '
                'Accepted values are: {}'.format(
                    self.BING_SRCTYPE_PARAMS.keys())
            )

        if srctype == 'Composite':
            if sources is None:
                raise BingError(
                    '`Composite` srctype requires the `sources` param as well')
            else:
                accepted = ('web', 'news', 'image', 'video', 'spell')
                received = sources.split('+')
                diff = set(received) - set(accepted)
                if diff:
                    raise BingError(
                        'Sources: `{}` not accepted. '.format(list(diff)) +
                        'Valid sources are: {}'.format(accepted)
                    )
        if extra:
            accepted = self.BING_SRCTYPE_PARAMS[srctype]
            diff = set(extra.keys()) - set(accepted)
            if diff:
                raise BingError(
                    'Parameters `{}` not accepted.'.format(list(diff))
                )

    def transform(self, results, srctype):
        """ Overwrite this method if you want your results transformed
            prior to being returned by the `query` method """
        return results

    def query(self, query, source_type='Web', skip=0, top=50, output_format='json', sources=None,
              **extra):
        """ Query the Bing API

            Params:
                `query`     : what to search for
                `srctype`   : the source type where to search
                `offset`    : results page offset
                `count`     : total number of results
                `sources`   : the sources where to look for results
                              e.g. 'web+news+spell'
                `**extra`   : other official Bing parameters """

        parameters = {'Query': enquote(query), '$top': top, '$skip': skip, '$format': output_format}
        url = self.BING_URL.format(source_type=source_type)


        # Check corectness
        self.check(source_type, sources, extra)

        # `Sources` required if srctype is `Composite`
        if source_type == 'Composite':
            parameters['Sources'] = enquote(sources)

        # Attach extra parameters
        if extra:
            for key, value in extra.items():
                parameters[key] = self.BING_SRCTYPE_PARAMS[source_type][key](value)

        data = self.fetch(url, self.key, parameters)

        if output_format == 'json':
            data = data.json()


        # Extract results
        results = None
        if source_type == 'Composite':
            results = data['d']['results'][0]
        else:
            results = data['d']['results']
        # print(results)
        return self.transform(results, source_type)

        