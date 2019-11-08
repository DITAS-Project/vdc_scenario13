# -*- coding: utf-8 -*-

import configparser
class Config(): 

    @classmethod
    def read(self, section, variable):
        parser = configparser.ConfigParser()
        parser.read(["config.ini"])
        return parser.get(section, variable)
