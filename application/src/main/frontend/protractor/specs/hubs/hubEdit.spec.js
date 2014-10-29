'use strict';

var po = require('../../pageobjects/pageobjects.js');
var fixtures = require('../../fixtures/fixtures');
var devApi = require('../devApi')();

describe('edit hub view', function () {
    var hubEditPage = po.hubEditPage({});
    var hubViewPage = po.hubViewPage({});

    describe('new hub', function () {
        beforeEach(function () {
            devApi.resetAll();
            hubEditPage.get();
        });

        describe('required fields', function () {
            it('name is required in all languages', function () {
                hubEditPage.setName("Hub name");
                hubEditPage.setNameFi("");
                expect(hubEditPage.isNameFiRequiredError()).toBe(true);
                expect(hubEditPage.isNameSvRequiredError()).toBe(false);
                expect(hubEditPage.isNameEnRequiredError()).toBe(false);

                hubEditPage.setName("Hub name");
                hubEditPage.setNameSv("");
                expect(hubEditPage.isNameFiRequiredError()).toBe(false);
                expect(hubEditPage.isNameSvRequiredError()).toBe(true);
                expect(hubEditPage.isNameEnRequiredError()).toBe(false);

                hubEditPage.setName("Hub name");
                hubEditPage.setNameEn("");
                expect(hubEditPage.isNameFiRequiredError()).toBe(false);
                expect(hubEditPage.isNameSvRequiredError()).toBe(false);
                expect(hubEditPage.isNameEnRequiredError()).toBe(true);
            });
        });
    });

    xdescribe('hub with 2 facilities', function () {
        var hubWithTwoFacilities = fixtures.hubsFixture.westend;

        beforeEach(function () {
            devApi.resetAll(hubWithTwoFacilities.facilities, [hubWithTwoFacilities]);
        });

        it('facility can be removed from hub', function () {
            hubEditPage.get(hubWithTwoFacilities.id);
            expect(hubEditPage.facilitiesTable.getSize()).toEqual(2);

            hubEditPage.toggleFacility(hubWithTwoFacilities.facilities[1]);

            /*
             * NOTE the asserts below occasionally fail on firefox; current best guess is that this is due to not all tiles loading.
             */
            expect(hubEditPage.facilitiesTable.isDisplayed()).toBe(true);
            expect(hubEditPage.facilitiesTable.getSize()).toEqual(1);

            hubEditPage.save();
            expect(hubViewPage.isDisplayed()).toBe(true);
            expect(hubViewPage.facilitiesTable.getSize()).toEqual(1);
        });
    });
});
