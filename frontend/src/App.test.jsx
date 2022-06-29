import { configure } from "enzyme";
import Adapter from "@wojtekmaj/enzyme-adapter-react-17";
configure({ adapter: new Adapter() });

describe("In the app", () => {
  test("Fake test", () => {
    expect(1).toEqual(1);
  });
});
