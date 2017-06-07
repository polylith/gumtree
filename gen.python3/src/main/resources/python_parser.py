import ast, asttokens
from ast import AST

import sys


def all_subclasses(cls):
    return cls.__subclasses__() + [g for s in cls.__subclasses__()
                                   for g in all_subclasses(s)]


types = all_subclasses(AST)
types.sort(key=lambda x: x.__name__)
skip_nodes = [ast.Load.__name__, ]


class Tree(object):
    def __init__(self, label: str, type_: int, type_label: str, pos: int = 0, length: int = 0, parent: "Tree" = None):
        self.label = label
        self.type = type_
        self.type_label = type_label
        self.pos = pos
        self.length = length
        self.parent = parent
        self.children = []

    def add_child(self, node):
        self.children.append(node)

    def to_xml(self, indent=0):
        print(
            '{indent}<tree type="{type}" label="{label}" typeLabel="{type_label}" pos="{pos}" length="{length}">'.format(
                indent="    " * indent,
                type=self.type,
                type_label=self.type_label,
                label=self.label,
                pos=self.pos,
                length=self.length,
            )
        )
        for child in self.children:
            child.to_xml(indent + 1)
        print("{indent}</tree>".format(indent="    " * indent))


class GumTreeVisitor(ast.NodeVisitor):
    def generic_visit(self, node, parent=None, field=""):
        label = ""
        tree = Tree(
            label=label,
            type_=types.index(node.__class__),
            type_label=node.__class__.__name__,
            parent=parent,
            pos=self.get_pos(node),
            length=self.get_length(node),
        )
        for field, value in ast.iter_fields(node):
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, ast.AST):
                        if item.__class__.__name__ in skip_nodes:
                            continue
                        self.generic_visit(item, parent=tree, field=field)
                    elif isinstance(value, str):
                        tree.label += value
            elif isinstance(value, ast.AST):
                if value.__class__.__name__ in skip_nodes:
                    continue
                self.generic_visit(value, parent=tree, field=field)
            elif isinstance(value, str):
                tree.label = value

        if not tree.label:
            tree.label = node.__class__.__name__

        if parent:
            parent.add_child(tree)
        else:
            tree.to_xml()

    def get_pos(self, node):
        try:
            return node.first_token.startpos
        except AttributeError:
            return 0

    def get_length(self, node):
        try:
            return node.last_token.endpos - node.first_token.startpos
        except AttributeError:
            return 0


if __name__ == '__main__':
    atok = asttokens.ASTTokens(open(sys.argv[1]).read(), parse=True)
    tree = atok.tree
    GumTreeVisitor().visit(tree)
