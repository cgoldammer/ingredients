import { configure } from "enzyme";
import Adapter from "@wojtekmaj/enzyme-adapter-react-17";
import { listElementsAreIdentical } from "./helpers/helpers";

configure({ adapter: new Adapter() });

describe("Testing helpers", () => {
  test("List elements identical", () => {
    const a = [1, 2];
    const b = [2, 1];
    expect(listElementsAreIdentical(a, b)).toEqual(true);
  });
  test("List elements non identical V1", () => {
    const a = [1, 2, 3];
    const b = [2, 1];
    expect(listElementsAreIdentical(a, b)).toEqual(false);
  });
  test("List elements non identical V2", () => {
    const a = [1, 2];
    const b = [3, 2, 1];
    expect(listElementsAreIdentical(a, b)).toEqual(false);
  });
});
