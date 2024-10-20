import { expect, beforeEach, test, suite, vi } from 'vitest';
import type { AxiosInstance } from 'axios';
import { type Resource, fetchWadlContent, extractResourceRoot } from './process-wadl';
import { JudoAxiosProvider } from '../src/services/data-axios/JudoAxiosProvider';
import { X_JUDO_SIGNED_IDENTIFIER, X_JUDO_COUNT_RECORDS, X_JUDO_MASK } from '../src/services/data-api/rest/headers';
import { GodServiceForGalaxiesImpl } from '../src/services/data-axios/GodServiceForGalaxiesImpl';
import { GodServiceForEarthImpl } from '../src/services/data-axios/GodServiceForEarthImpl';
import { ViewGalaxyServiceImpl } from '../src/services/data-axios/ViewGalaxyServiceImpl';
import type { JudoIdentifiable } from '../src/services/data-api/common/JudoIdentifiable';
import type { ViewCreature } from '../src/services/data-api/model/ViewCreature';
import type { ViewGalaxy, ViewGalaxyStored } from '../src/services/data-api/model/ViewGalaxy';

const ORIGIN = 'http://tatami-tests.judo.technology';
const API_DEFAULT_BASE_URL = ORIGIN;
const API_RELATIVE_PATH = '/api';
const FILE_DEFAULT_BASE_URL = ORIGIN;

suite('API Tests', () => {
    const xml = fetchWadlContent('./tests/actiongrouptest-wadl.xml');
    const mappedXml: Resource[] = extractResourceRoot(xml.application.resources.resource, []);

    let axiosPostMock = vi.fn().mockImplementation(async () => ({ data: {} }));
    let axiosGetMock = vi.fn().mockImplementation(async () => ({ data: {} }));
    let axiosMock: Partial<AxiosInstance> = {
        post: axiosPostMock,
        get: axiosGetMock,
        defaults: {
            headers: {
                post: {},
            } as any,
        },
    };
    let judoAxiosProvider: JudoAxiosProvider;

    beforeEach(() => {
        axiosPostMock.mockClear();
        axiosGetMock.mockClear();
        judoAxiosProvider = new JudoAxiosProvider()
        judoAxiosProvider.init({
            axios: axiosMock as AxiosInstance,
            basePathFactory: () => API_DEFAULT_BASE_URL + API_RELATIVE_PATH,
            filePathFactory: () => FILE_DEFAULT_BASE_URL + '/ActionGroupTest',
        });
    });

    test('Singleton Access GET request is mapped properly', async () => {
        const getSignedId: Resource = mappedXml.find(r => r.path === 'God/God/earth/~get');
        const getInstance: Resource = mappedXml.find(r => r.path === 'View/Planet/~get');
        const godServiceForEarthImpl = new GodServiceForEarthImpl(judoAxiosProvider);

        assertSinglePostResource(getSignedId);
        assertSinglePostResource(getInstance);

        await godServiceForEarthImpl.refreshForEarth();

        assertSinglePostCall(getSignedId, {}, undefined);

        axiosPostMock.mockClear(); // clear mock to reset counter, etc...

        await godServiceForEarthImpl.refresh();

        assertSinglePostCall(getInstance, {}, undefined);
    });

    test('Singleton Access View - operation call on creatures row', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'View/Creature/hateGod');
        const godServiceForEarthImpl = new GodServiceForEarthImpl(judoAxiosProvider);
        const target: JudoIdentifiable<ViewCreature> = {
            __signedIdentifier: 'abc',
        };

        assertSinglePostResource(resource);

        await godServiceForEarthImpl.hateGodForCreatures(target);

        assertSinglePostCall(resource, undefined, {
            'headers': {
                [X_JUDO_SIGNED_IDENTIFIER]: target.__signedIdentifier,
            },
        });
    });

    test('List Access POST request is mapped properly', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'God/God/galaxies/~list');
        const godServiceForGalaxiesImpl = new GodServiceForGalaxiesImpl(judoAxiosProvider);

        assertSinglePostResource(resource);

        await godServiceForGalaxiesImpl.list();

        assertSinglePostCall(resource, {});
    });

    test('List Access POST request with total count flag', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'God/God/galaxies/~list');
        const godServiceForGalaxiesImpl = new GodServiceForGalaxiesImpl(judoAxiosProvider);

        assertSinglePostResource(resource);

        await godServiceForGalaxiesImpl.list(undefined, undefined, {
            [X_JUDO_COUNT_RECORDS]: 'true',
        });

        assertSinglePostCall(resource, {}, {
            headers: {
                [X_JUDO_COUNT_RECORDS]: 'true',
            },
        });
    });

    test('View Access POST request is mapped properly', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'View/Galaxy/~get');
        const viewGalaxyServiceImpl = new ViewGalaxyServiceImpl(judoAxiosProvider);
        const target: JudoIdentifiable<ViewGalaxy> = {
            __signedIdentifier: 'abc',
        };

        assertSinglePostResource(resource);

        await viewGalaxyServiceImpl.refresh(target, {});

        assertSinglePostCall(resource, {}, {
            'headers': {
                [X_JUDO_SIGNED_IDENTIFIER]: target.__signedIdentifier,
            },
        });
    });

    test('Default create Access POST request is mapped properly', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'God/God/galaxies/~create');
        const godServiceForGalaxiesImpl = new GodServiceForGalaxiesImpl(judoAxiosProvider);
        const target: ViewGalaxy = {
            name: 'abc',
            magnitude: 11,
            constellation: 'test',
        };

        assertSinglePostResource(resource);

        await godServiceForGalaxiesImpl.create(target);

        assertSinglePostCall(resource, target, {
            'headers': {
                [X_JUDO_MASK]: '{}',
            },
        });
    });

    test('Modified update Access POST request is mapped properly', async () => {
        const resource: Resource = mappedXml.find(r => r.path === 'View/Galaxy/~update');
        const viewGalaxyServiceImpl = new ViewGalaxyServiceImpl(judoAxiosProvider);
        const target: Partial<ViewGalaxyStored> = {
            __signedIdentifier: 'abc',
            magnitude: 222,
        };

        assertSinglePostResource(resource);

        await viewGalaxyServiceImpl.update(target, { _mask: '{magnitude}' });

        assertSinglePostCall(resource, target, {
            'headers': {
                [X_JUDO_SIGNED_IDENTIFIER]: target.__signedIdentifier,
                [X_JUDO_MASK]: '{magnitude}',
            },
        });
    });

    function assertSinglePostResource(resource: Resource): void {
        expect(resource).toBeDefined();
        expect(resource.methods.length).toBe(1);
        expect(resource.methods[0].name).toBe('POST');
    }

    function assertSinglePostCall(resource: Resource, payload?: any, options?: any, skipHeaders = false): void {
        expect(axiosPostMock).toHaveBeenCalledTimes(1);
        // test tool is sensitive for undefined parameters, in certain cases we skip the 3rd param, and are not passing undefiend
        if (skipHeaders) {
            expect(axiosPostMock).toHaveBeenCalledWith(xml.application.resources.base + '/' + resource.path, payload);
        } else {
            expect(axiosPostMock).toHaveBeenCalledWith(xml.application.resources.base + '/' + resource.path, payload, options);
        }
    }
});
