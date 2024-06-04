import { expect, test, suite } from 'vitest';
import { draftIdentifierPrefix, type SerializerUtil } from '../src/services/data-api/common/Serializer';
import type { ViewGalaxyStored } from '../src/services/data-api/model/ViewGalaxy';
import { ViewGalaxyStoredSerializer } from '../src/services/data-api/rest/ViewGalaxySerializer';

suite('Serialization Tests', () => {
    const util: Partial<SerializerUtil> = {
        serializeDate: (date: Date) => date.toISOString().substring(0, 10),
        deserializeDate: (date: any) => new Date(date as string),
    };
    const galaxySerializer = new ViewGalaxyStoredSerializer(util as SerializerUtil);

    const serialized = Object.freeze({
        __signedIdentifier: 'aaa',
        astronomer: {
            __signedIdentifier: 'ttt',
            born: '2022-03-25',
            name: 'bello'
        },
        constellation: 'test const',
        magnitude: 1.22,
        nakedEye: true,
        name: 'hello',
        stars: [
            {
                __signedIdentifier: 'ccc',
                name: 'yoo',
            },
            {
                __signedIdentifier: 'ggg',
                name: 'boo'
            },
        ],
    });
    const galaxy: ViewGalaxyStored = {
        __signedIdentifier: 'aaa',
        name: 'hello',
        constellation: 'test const',
        nakedEye: true,
        magnitude: 1.22,
        stars: [
            {
                __signedIdentifier: 'ccc',
                name: 'yoo',
            },
            {
                __signedIdentifier: 'ggg',
                name: 'boo',
            },
        ],
        astronomer: {
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
        const result = galaxySerializer.serialize(galaxyToClean, true);
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
