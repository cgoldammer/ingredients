import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import Container from "@mui/material/Container";
import Avatar from "@mui/material/Avatar";
import Button from "@mui/material/Button";
import Tooltip from "@mui/material/Tooltip";
import MenuItem from "@mui/material/MenuItem";
import AdbIcon from "@mui/icons-material/Adb";
import SettingsOutlinedIcon from "@mui/icons-material/Settings";
import { setToken } from "../userReducer";

import Link from "@mui/material/Link";
import { Link as RouterLink } from "react-router-dom";
import { useLogoutMutation } from "./api/apiSlice";
import { useDispatch, useSelector } from "react-redux";
import { userSelector } from "../store";

const pagesData = [{ name: "Search", url: "/" }];
const pagesDataNoUser = [{ name: "Register or Login", url: "/register" }];

const settingsData = [{ name: "About", url: "/about" }];

const settingsDataUser = [
  { name: "Logout", submit: "/logout" },
  { name: "Profile", url: "/profile" },
];

export function TopMenu() {
  const [anchorElNav, setAnchorElNav] = React.useState(null);
  const [anchorElUser, setAnchorElUser] = React.useState(null);
  const dispatch = useDispatch();
  const [logout, { error: logoutError }] = useLogoutMutation();
  const user = useSelector(userSelector);

  const pagesDataAll = pagesData.concat(
    user == undefined ? pagesDataNoUser : []
  );

  const settingsDataAll = settingsData.concat(
    user != undefined ? settingsDataUser : []
  );

  const handleOpenNavMenu = (event) => {
    setAnchorElNav(event.currentTarget);
  };
  const handleOpenUserMenu = (event) => {
    setAnchorElUser(event.currentTarget);
  };

  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  const handleCloseUserMenu = () => {
    setAnchorElUser(null);
  };

  const getPagesItem = (pageData) => {
    const { name, url } = pageData;
    var item = (
      <MenuItem
        key={name}
        onClick={handleCloseUserMenu}
        component={RouterLink}
        to={url}
      >
        <Typography textAlign="center">{name}</Typography>
      </MenuItem>
    );
    return item;
  };

  const getMenuItem = (settingsData) => {
    const { name, url, submit } = settingsData;
    var item = (
      <MenuItem
        key={name}
        onClick={() => {
          logout().then(() => {
            dispatch(setToken(undefined));
          });
          handleCloseUserMenu();
        }}
      >
        <Typography textAlign="center">{name}</Typography>
      </MenuItem>
    );
    if (url != undefined) {
      item = (
        <MenuItem key={name} onClick={handleCloseUserMenu}>
          <RouterLink to={url}>
            <Typography textAlign="center">{name} </Typography>
          </RouterLink>
        </MenuItem>
      );
    }
    return item;
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <Typography
            variant="h6"
            sx={{
              display: "flex",
              fontWeight: 700,
            }}
          >
            Cocktails
          </Typography>
          <Box sx={{ flexGrow: 1, display: "flex" }}>
            {pagesDataAll.map(getPagesItem)}
          </Box>

          <Box sx={{ flexGrow: 1 }}>
            <Tooltip title="Open settings">
              <IconButton onClick={handleOpenUserMenu} sx={{ p: 0 }}>
                <SettingsOutlinedIcon />
              </IconButton>
            </Tooltip>
            <Menu
              sx={{ mt: "45px" }}
              id="menu-appbar"
              anchorEl={anchorElUser}
              anchorOrigin={{
                vertical: "top",
                horizontal: "right",
              }}
              keepMounted
              transformOrigin={{
                vertical: "top",
                horizontal: "right",
              }}
              open={Boolean(anchorElUser)}
              onClose={handleCloseUserMenu}
            >
              {settingsDataAll.map(getMenuItem)}
            </Menu>
          </Box>
        </Toolbar>
      </AppBar>
    </Box>
  );
}
