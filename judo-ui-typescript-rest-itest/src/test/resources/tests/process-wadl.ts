import { readFileSync } from 'node:fs';
import { normalize } from 'node:path';
import { XMLParser } from 'fast-xml-parser';

export function fetchWadlContent(source: string): any {
    try {
        const xmlData = readFileSync(normalize(source));
        const parser = new XMLParser({
            ignoreAttributes : false,
            attributeNamePrefix : '',
        });
        const data: any = parser.parse(xmlData.toString());
        return data;
    } catch (e) {
        console.error(e);
        return null;
    }
}

export type MethodName = 'get' | 'post' | 'delete' | 'patch';

export interface Param {
    type: string;
    name: string;
}

export interface Request {
    params: Param[];
    representation: string;
}

export interface Response {
    representation: string;
}

export interface Method {
    name: MethodName;
    request?: Request;
    response?: Response;
}

export interface Resource {
    path: string;
    methods: Method[];
}

// export function extractAllResources(xml: any, resources: Resource[] = []): Resource[] {
//     for (const key in xml) {
//         if (key === 'resources') {
//             extractResourceRoot(xml[key], resources);
//             if (xml[key].resource) {
//                 extractResourceRoot(xml[key].resource, resources);
//             }
//         }
//     }
//     return resources;
// }

export function extractResourceRoot(input: any, resources: Resource[]) {
    if (Array.isArray(input)) {
        for (const res of input) {
            extractResource(res, resources);
        }
    } else if (typeof input === 'object') {
        extractResource(input, resources);
    }
    return resources;
}

export function extractResource(res: any, resources: Resource[] = []) {
    const resource: Resource = {
        path: res.path,
        methods: [],
    };
    resources.push(resource);

    if (res.method) {
        if (Array.isArray(res.method)) {
            for (const mtd of res.method) {
                extractMethod(mtd, resource);
            }
        } else if (typeof res.method === 'object') {
            extractMethod(res.method, resource);
        }
    }

    if (typeof res.resource === 'object') {
        extractResourceRoot(res.resource, resources);
    }

    // const nested = extractAllResources(res);

    // resources.push(...nested);
}

function extractMethod(mtd: any, resource: Resource) {
    const method: Method = {
        name: mtd.name,
        request: {
            representation: mtd.representation ? mtd.representation.mediaType : undefined,
            params: []
        },
    };
    if (mtd?.request?.param) {
        fillMethod(mtd?.request?.param, method);
    }
    resource.methods.push(method);
}

function fillMethod(param: any, method: Method) {
    if (Array.isArray(param)) {
        for (const prm of param) {
            const p: Param = {
                name: prm.name,
                type: prm.type,
            };
            method.request.params.push(p);
        }
    } else if (typeof param === 'object') {
        const p: Param = {
            name: param.name,
            type: param.type,
        };
        method.request.params.push(p);
    }
}
