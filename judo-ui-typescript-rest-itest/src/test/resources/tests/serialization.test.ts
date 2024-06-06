import { expect, test, suite } from 'vitest';
import { draftIdentifierPrefix } from '../src/services/data-api/common/utils';
import type { ViewGalaxyStored } from '../src/services/data-api/model/ViewGalaxy';
import { ViewGalaxyStoredSerializer } from '../src/services/data-api/rest/ViewGalaxySerializer';

suite('Serialization Tests', () => {
    const galaxySerializer = new ViewGalaxyStoredSerializer();

    const serialized = Object.freeze({
        __identifier: 'id1',
        __signedIdentifier: 'aaa',
        astronomer: Object.freeze({
            __identifier: 'id4',
            __signedIdentifier: 'ttt',
            born: '2022-03-25',
            name: 'bello'
        }),
        constellation: 'test const',
        magnitude: 1.22,
        nakedEye: true,
        name: 'hello',
        stars: [
            Object.freeze({
                __identifier: 'id2',
                __signedIdentifier: 'ccc',
                name: 'yoo',
            }),
            Object.freeze({
                __identifier: 'id3',
                __signedIdentifier: 'ggg',
                name: 'boo'
            }),
        ],
    });
    const galaxy: ViewGalaxyStored = {
        __identifier: 'id1',
        __signedIdentifier: 'aaa',
        name: 'hello',
        constellation: 'test const',
        nakedEye: true,
        magnitude: 1.22,
        stars: [
            {
                __identifier: 'id2',
                __signedIdentifier: 'ccc',
                name: 'yoo',
            },
            {
                __identifier: 'id3',
                __signedIdentifier: 'ggg',
                name: 'boo',
            },
        ],
        astronomer: {
            __identifier: 'id4',
            __signedIdentifier: 'ttt',
            name: 'bello',
            born: new Date('2022-03-25'),
        },
    };

    test('Serialization with nested relations', async () => {
        const result = galaxySerializer.serialize(galaxy);
        expect(result).toEqual(serialized);
    });

    test('Cleanup', async () => {
        const galaxyToClean: ViewGalaxyStored = {
            __identifier: `${draftIdentifierPrefix}asdasd`,
            __signedIdentifier: 'bbb',
            name: 'hello',
            constellation: 'test const',
        };
        const result = galaxySerializer.serialize(galaxyToClean);
        expect(result).toEqual({
            __signedIdentifier: 'bbb',
            name: 'hello',
            constellation: 'test const',
        });
    });

    test('Deserialize withn ested relations', async () => {
        const result = galaxySerializer.deserialize(serialized);
        expect(result).toEqual(galaxy);
    });
});
